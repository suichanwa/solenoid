#!/usr/bin/env python3
"""
Minecraft texture rescaler.

Drop images into ./input, run this script, get game-ready textures in ./output.
Output filenames are auto-lowercased and sanitized (Minecraft requires lowercase,
no spaces/special chars).

MODES (pick one; default is --block):
  --block          Opaque block texture: downscale + flatten palette. No transparency.
  --item           Item texture: removes the background to transparency, crops to the
                   object, centers it, downscale + hard alpha edges. (the "rescale +
                   remove background" mode)
  --rescale-only   Just downscale. No palette flattening, no background removal.

OPTIONS:
  --size N         Target square size in px (default 16).
  --colors N       Flatten to N colors, no dithering (default 8; use 0 to disable).
  --method M       Downscale filter: nearest | box | lanczos (default nearest = crisp).
  --contrast F     Contrast multiplier, e.g. 1.2 (default 1.0 = none).
  --crop           (block mode) also trim a uniform border/frame if present.
  --file NAME      Process only this one file in ./input (default: all images).

EXAMPLES:
  python rescale.py --block                 # cable, machine faces, ores...
  python rescale.py --item                  # magnet, coil, ingot... (transparent bg)
  python rescale.py --item --size 32        # same, at 32x32
  python rescale.py --block --crop --contrast 1.2
  python rescale.py --rescale-only --size 32 --method box
"""
import argparse, os, re, sys
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

def remove_bg(img):
    """Make the border-connected bright/uniform background transparent.
    Border-flood so interior light pixels (e.g. white highlights) survive."""
    try:
        from scipy import ndimage
    except ImportError:
        sys.exit("--item needs scipy:  pip install scipy")
    a = np.asarray(img.convert("RGB")).astype(np.int16)
    mx, mn = a.max(2), a.min(2)
    cand = ((mx - mn) <= 14) & (mn >= 180)        # near-neutral AND bright = bg (white/checker)
    lbl, _ = ndimage.label(cand)
    edge = set(np.unique(np.concatenate([lbl[0, :], lbl[-1, :], lbl[:, 0], lbl[:, -1]])))
    edge.discard(0)
    bg = np.isin(lbl, list(edge))
    alpha = np.where(bg, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([a.astype(np.uint8), alpha]), "RGBA")

def crop_to_content(img):
    """Crop to alpha bbox (RGBA) or to a non-uniform-border bbox (RGB)."""
    if img.mode == "RGBA":
        ys, xs = np.where(np.asarray(img)[..., 3] > 0)
    else:
        a = np.asarray(img.convert("RGB")).astype(int)
        diff = np.abs(a - a[0, 0]).sum(2)         # distance from corner color
        ys, xs = np.where(diff > 30)
    if len(xs) == 0:
        return img
    return img.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))

def quantize_keep_alpha(img, colors):
    if colors <= 0:
        return img
    if img.mode == "RGBA":
        a = np.asarray(img)
        rgb = Image.fromarray(a[..., :3], "RGB").quantize(colors=colors, dither=Image.NONE).convert("RGB")
        return Image.fromarray(np.dstack([np.asarray(rgb), a[..., 3]]), "RGBA")
    return img.quantize(colors=colors, dither=Image.NONE).convert("RGB")

def process(path, args):
    img = Image.open(path)
    f = FILT[args.method]

    if args.mode == "item":
        img = crop_to_content(remove_bg(img))
        w, h = img.size
        s = max(1, args.size - 2)                 # 1px breathing room
        sc = min(s / w, s / h)
        nw, nh = max(1, round(w * sc)), max(1, round(h * sc))
        r = img.resize((nw, nh), f)
        canvas = Image.new("RGBA", (args.size, args.size), (0, 0, 0, 0))
        canvas.paste(r, ((args.size - nw) // 2, (args.size - nh) // 2), r)
        arr = np.asarray(canvas).copy()
        arr[..., 3] = np.where(arr[..., 3] >= 128, 255, 0)   # hard alpha edges
        out = Image.fromarray(arr, "RGBA")
    else:
        img = img.convert("RGB")
        if args.mode == "block" and args.crop:
            img = crop_to_content(img)
        out = img.resize((args.size, args.size), f)

    if args.contrast != 1.0:
        out = ImageEnhance.Contrast(out).enhance(args.contrast)
    if args.mode != "rescale-only":
        out = quantize_keep_alpha(out, args.colors)
    return out

def main():
    ap = argparse.ArgumentParser(description="Minecraft texture rescaler", add_help=True)
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
