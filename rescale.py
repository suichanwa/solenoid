#!/usr/bin/env python3
"""
Advanced Minecraft Texture Rescaler with Multi-Pass Background Peeling.
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
    """Peels away the outermost background layer by sampling all 4 corners 
    and flood-filling matching border-connected colors."""
    try:
        from scipy import ndimage
    except ImportError:
        sys.exit("--item needs scipy:  pip install scipy")
        
    rgb_img = img.convert("RGB")
    a = np.asarray(rgb_img).astype(np.int16)
    h, w, _ = a.shape
    
    # Sample all 4 corners to capture multi-toned or split backgrounds safely
    corners = [a[0, 0], a[0, w-1], a[h-1, 0], a[h-1, w-1]]
    
    bg_mask = np.zeros((h, w), dtype=bool)
    for color in corners:
        dist = np.abs(a - color).sum(axis=2)
        bg_mask |= (dist < 55)  # Generous threshold to clear compression noise & split-tones
        
    # Also flag near-black drop shadows hovering right on the edge boundaries
    brightness = a.mean(axis=2)
    bg_mask |= (brightness < 35)
    
    # Label connected components to isolate true border backgrounds
    lbl, _ = ndimage.label(bg_mask)
    
    # Find background blocks touching ANY edge of the frame
    border = np.concatenate([lbl[0, :], lbl[-1, :], lbl[:, 0], lbl[:, -1]])
    edge_labels = set(np.unique(border))
    edge_labels.discard(0)
    
    bg = np.isin(lbl, list(edge_labels))
    
    # Build the alpha channel, blending with any alpha layers from a previous pass
    if img.mode == "RGBA":
        old_alpha = np.asarray(img)[..., 3]
        alpha = np.where(bg | (old_alpha == 0), 0, 255).astype(np.uint8)
    else:
        alpha = np.where(bg, 0, 255).astype(np.uint8)
        
    return Image.fromarray(np.dstack([a.astype(np.uint8), alpha]), "RGBA")

def crop_to_content(img):
    """Crop tightly to the remaining non-transparent item content."""
    if img.mode == "RGBA":
        ys, xs = np.where(np.asarray(img)[..., 3] > 0)
    else:
        a = np.asarray(img.convert("RGB")).astype(int)
        diff = np.abs(a - a[0, 0]).sum(2)
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
        # Run a 2-pass peeling sequence to completely strip nested boxes
        for _ in range(2):
            img = remove_bg(img)
            img = crop_to_content(img)
            
        w, h = img.size
        s = max(1, args.size - 2)                 # 1px padding breathing room
        sc = min(s / w, s / h)
        nw, nh = max(1, round(w * sc)), max(1, round(h * sc))
        r = img.resize((nw, nh), f)
        canvas = Image.new("RGBA", (args.size, args.size), (0, 0, 0, 0))
        canvas.paste(r, ((args.size - nw) // 2, (args.size - nh) // 2), r)
        arr = np.asarray(canvas).copy()
        arr[..., 3] = np.where(arr[..., 3] >= 128, 255, 0)   # Perfect crisp edges
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