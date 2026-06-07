#!/usr/bin/env python3
"""
Minecraft Texture Rescaler.

Handles raw AI-generated art (large, shaded, often with a baked-in fake-transparency
checkerboard or a solid background) and turns it into a clean NxN game texture.

Key behaviors:
  * --item  : if the source already has real alpha, use it; otherwise auto-detect and
              strip the baked background (checkerboard OR solid), crop, center.
  * downscale uses AREA AVERAGING (premultiplied) when shrinking hard, because
              point-sampling a 2000px source down to 16px just scatters noise.
  * quantize to a small palette (dither off) -> flat, vanilla-style color regions.
  * despeckle : removes isolated stray pixels left over after quantize.
"""
import argparse, os, re, sys
from collections import Counter
import numpy as np
from PIL import Image, ImageEnhance

HERE = os.path.dirname(os.path.abspath(__file__))
IN  = os.path.join(HERE, "input")
OUT = os.path.join(HERE, "output")
FILT = {"nearest": Image.NEAREST, "box": Image.BOX, "lanczos": Image.LANCZOS}
EXTS = (".png", ".jpg", ".jpeg", ".webp", ".bmp")


def sanitize(name):
    base = os.path.splitext(name)[0]
    base = re.sub(r"[^a-z0-9_]+", "_", base.lower()).strip("_") or "texture"
    return base + ".png"


def has_real_alpha(img):
    """True if the source already ships a transparent background."""
    if img.mode in ("RGBA", "LA"):
        return int(np.asarray(img.convert("RGBA"))[..., 3].min()) < 250
    return img.mode == "P" and "transparency" in img.info


def normalize_alpha(img, thresh=128):
    """Binarize an existing alpha channel to crisp on/off."""
    img = img.convert("RGBA")
    a = np.asarray(img).copy()
    a[..., 3] = np.where(a[..., 3] >= thresh, 255, 0)
    return Image.fromarray(a, "RGBA")


def detect_bg_tones(a, tol=20, min_frac=0.02):
    """Find the dominant colors along the border (the baked background).
    Returns a list of RGB tones (handles a 2-tone checkerboard or a solid fill)."""
    h, w, _ = a.shape
    bw, bh = max(2, w // 20), max(2, h // 20)
    border = np.concatenate([
        a[:bh].reshape(-1, 3), a[-bh:].reshape(-1, 3),
        a[:, :bw].reshape(-1, 3), a[:, -bw:].reshape(-1, 3),
    ])
    binned = (border // 8) * 8
    counts = Counter(map(tuple, binned))
    total = len(border)
    tones = []
    for col, n in counts.most_common():
        if n / total < min_frac:
            break
        col = np.array(col, dtype=np.int16)
        if all(np.abs(col - t).max() > tol for t in tones):
            tones.append(col)
        if len(tones) >= 4:
            break
    return tones


def remove_baked_bg(img, tol=22):
    """Strip a baked-in background (checkerboard or solid) by border flood-fill.
    A pixel is background only if it's within `tol` (per channel) of a detected
    border tone AND its region connects to the frame edge, so interior highlights
    that happen to match a tone are preserved."""
    try:
        from scipy import ndimage
    except ImportError:
        sys.exit("--item needs scipy:  pip install scipy")

    a = np.asarray(img.convert("RGB")).astype(np.int16)
    h, w, _ = a.shape
    tones = detect_bg_tones(a, tol=tol)
    if not tones:
        return img.convert("RGBA")

    mask = np.zeros((h, w), dtype=bool)
    for t in tones:
        mask |= (np.abs(a - t).max(axis=2) <= tol)

    # Bridge the thin anti-aliased seams between checkerboard squares so the
    # whole grid is one connected region.
    # border_value=1 so the closing's erosion step doesn't strip the outer ring
    # of the mask (which would detach the background from the frame edge).
    mask = ndimage.binary_closing(mask, structure=np.ones((3, 3), bool),
                                  iterations=2, border_value=1)

    lbl, _ = ndimage.label(mask)
    border = np.concatenate([lbl[0, :], lbl[-1, :], lbl[:, 0], lbl[:, -1]])
    edge_labels = set(np.unique(border)); edge_labels.discard(0)
    bg = np.isin(lbl, list(edge_labels))

    alpha = np.where(bg, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([a.astype(np.uint8), alpha]), "RGBA")


def crop_to_content(img):
    if img.mode == "RGBA":
        ys, xs = np.where(np.asarray(img)[..., 3] > 0)
    else:
        a = np.asarray(img.convert("RGB")).astype(int)
        diff = np.abs(a - a[0, 0]).sum(2)
        ys, xs = np.where(diff > 30)
    if len(xs) == 0:
        return img
    return img.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def resize_premult(img, size, rf):
    """Alpha-correct (premultiplied) resize so transparent pixels' RGB don't
    bleed dark halos into the edges when area-averaging."""
    arr = np.asarray(img.convert("RGBA")).astype(np.float32)
    al = arr[..., 3:4] / 255.0
    arr[..., :3] *= al
    pm = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").resize(size, rf)
    p = np.asarray(pm).astype(np.float32)
    a2 = p[..., 3:4] / 255.0
    safe = np.where(a2 > 0, a2, 1.0)
    p[..., :3] = np.clip(p[..., :3] / safe, 0, 255)
    return Image.fromarray(p.astype(np.uint8), "RGBA")


def quantize_keep_alpha(img, colors):
    if colors <= 0:
        return img
    if img.mode == "RGBA":
        a = np.asarray(img)
        rgb = Image.fromarray(a[..., :3], "RGB").quantize(colors=colors, dither=Image.NONE).convert("RGB")
        return Image.fromarray(np.dstack([np.asarray(rgb), a[..., 3]]), "RGBA")
    return img.quantize(colors=colors, dither=Image.NONE).convert("RGB")


def despeckle(img, passes=2):
    """Remove isolated stray pixels: any opaque pixel whose color is shared by
    fewer than 3 of its 8 opaque neighbors gets replaced by the dominant neighbor."""
    rgba = np.asarray(img.convert("RGBA")).copy()
    h, w, _ = rgba.shape
    for _ in range(max(0, passes)):
        out = rgba.copy()
        for y in range(h):
            for x in range(w):
                if rgba[y, x, 3] == 0:
                    continue
                neigh = []
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        if dx == 0 and dy == 0:
                            continue
                        ny, nx = y + dy, x + dx
                        if 0 <= ny < h and 0 <= nx < w and rgba[ny, nx, 3] > 0:
                            neigh.append(tuple(int(v) for v in rgba[ny, nx, :3]))
                if len(neigh) < 4:
                    continue
                cur = tuple(int(v) for v in rgba[y, x, :3])
                same = sum(1 for c in neigh if c == cur)
                if same < 3:
                    dom, cnt = Counter(neigh).most_common(1)[0]
                    if cnt >= 4:
                        out[y, x, :3] = dom
        rgba = out
    result = Image.fromarray(rgba, "RGBA")
    return result.convert("RGB") if img.mode == "RGB" else result


def _down_filter(method, src_wh, dst_wh):
    """Use area-average (BOX) instead of NEAREST when shrinking hard — NEAREST
    point-sampling a huge source is the #1 cause of speckle."""
    down = max(src_wh[0] / max(1, dst_wh[0]), src_wh[1] / max(1, dst_wh[1]))
    if FILT[method] == Image.NEAREST and down > 2:
        return Image.BOX
    return FILT[method]


def process(path, args):
    img = Image.open(path)

    if args.mode == "item":
        img = normalize_alpha(img) if has_real_alpha(img) else remove_baked_bg(img)
        img = crop_to_content(img)

        w, h = img.size
        s = max(1, args.size - 2)                 # 1px breathing room
        sc = min(s / w, s / h)
        nw, nh = max(1, round(w * sc)), max(1, round(h * sc))
        rf = _down_filter(args.method, (w, h), (nw, nh))
        r = resize_premult(img, (nw, nh), rf)

        canvas = Image.new("RGBA", (args.size, args.size), (0, 0, 0, 0))
        canvas.paste(r, ((args.size - nw) // 2, (args.size - nh) // 2), r)
        arr = np.asarray(canvas).copy()
        arr[..., 3] = np.where(arr[..., 3] >= 128, 255, 0)   # crisp edges
        out = Image.fromarray(arr, "RGBA")

        if args.contrast != 1.0:
            rgb = ImageEnhance.Contrast(out.convert("RGB")).enhance(args.contrast)
            out = Image.fromarray(np.dstack([np.asarray(rgb), np.asarray(out)[..., 3]]), "RGBA")
        out = quantize_keep_alpha(out, args.colors)
        if not args.no_clean:
            out = despeckle(out, passes=args.clean_passes)
        return out

    # block / rescale-only
    img = img.convert("RGB")
    if args.mode == "block" and args.crop:
        img = crop_to_content(img)
    w, h = img.size
    rf = _down_filter(args.method, (w, h), (args.size, args.size)) if args.mode == "block" else FILT[args.method]
    out = img.resize((args.size, args.size), rf)
    if args.contrast != 1.0:
        out = ImageEnhance.Contrast(out).enhance(args.contrast)
    if args.mode != "rescale-only":
        out = quantize_keep_alpha(out, args.colors)
    if args.clean and args.mode == "block":
        out = despeckle(out, passes=args.clean_passes)
    return out


def main():
    ap = argparse.ArgumentParser(description="Minecraft texture rescaler")
    g = ap.add_mutually_exclusive_group()
    g.add_argument("--block", dest="mode", action="store_const", const="block")
    g.add_argument("--item", dest="mode", action="store_const", const="item")
    g.add_argument("--rescale-only", dest="mode", action="store_const", const="rescale-only")
    ap.set_defaults(mode="block")
    ap.add_argument("--size", type=int, default=16)
    ap.add_argument("--colors", type=int, default=8)
    ap.add_argument("--method", choices=list(FILT), default="nearest")
    ap.add_argument("--contrast", type=float, default=1.0)
    ap.add_argument("--crop", action="store_true")
    ap.add_argument("--no-clean", action="store_true", help="disable despeckle (item mode)")
    ap.add_argument("--clean", action="store_true", help="enable despeckle for block mode too")
    ap.add_argument("--clean-passes", type=int, default=2)
    ap.add_argument("--file", default=None)
    args = ap.parse_args()

    os.makedirs(IN, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    files = [args.file] if args.file else sorted(os.listdir(IN))
    n = 0
    for fn in files:
        if not fn.lower().endswith(EXTS):
            continue
        src = os.path.join(IN, fn)
        if not os.path.isfile(src):
            print(f"skip (not found): {fn}"); continue
        try:
            out = process(src, args)
            dst = os.path.join(OUT, sanitize(fn))
            out.save(dst)
            print(f"{fn} -> {os.path.basename(dst)}  [{out.size[0]}x{out.size[1]}, {args.mode}]")
            n += 1
        except Exception as e:
            print(f"FAILED {fn}: {e}")
    print(f"\ndone: {n} file(s) written to {OUT}")


if __name__ == "__main__":
    main()
