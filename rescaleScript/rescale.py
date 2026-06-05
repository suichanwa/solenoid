#!/usr/bin/env python3
"""Texture-processing tool for finishing AI-generated Minecraft textures.

Processes every image in input/ and writes Minecraft-ready PNGs to output/.

Modes:
  --block (default)  opaque block texture (RGB, no transparency)
  --item             item texture with transparent background (border-flood removal)
  --rescale-only     just downscale, nothing else

See README.md for details.
"""

import argparse
import os
import re
import sys

from PIL import Image, ImageEnhance
import numpy as np
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
INPUT_DIR = os.path.join(HERE, "input")
OUTPUT_DIR = os.path.join(HERE, "output")

EXTS = (".png", ".jpg", ".jpeg", ".webp", ".bmp")

RESAMPLE = {
    "nearest": Image.NEAREST,
    "box": Image.BOX,
    "lanczos": Image.LANCZOS,
}

# Background-mask thresholds (item mode)
NEUTRAL_MAX_SPREAD = 14   # max(R,G,B) - min(R,G,B) <= this  -> near-neutral
BRIGHT_MIN_CHANNEL = 180  # min(R,G,B) >= this               -> bright


def sanitize_name(name):
    """Lowercase, replace any char not in [a-z0-9_] with '_', force .png."""
    stem = os.path.splitext(name)[0].lower()
    stem = re.sub(r"[^a-z0-9_]", "_", stem)
    return stem + ".png"


def apply_contrast(img, factor):
    if factor == 1.0:
        return img
    return ImageEnhance.Contrast(img).enhance(factor)


def quantize_flat(img_rgb, colors):
    """Flat-palette quantize, no dithering. colors<=0 disables."""
    if not colors or colors <= 0:
        return img_rgb
    q = img_rgb.quantize(colors=colors, dither=Image.NONE)
    return q.convert("RGB")


def downscale(img, size, method):
    return img.resize((size, size), RESAMPLE[method])


def auto_border_crop(img_rgb):
    """Trim a uniform border/frame if present. Returns cropped RGB image."""
    arr = np.asarray(img_rgb)
    h, w = arr.shape[:2]
    # corner colors; if all four corners match, treat as border color
    corners = [arr[0, 0], arr[0, w - 1], arr[h - 1, 0], arr[h - 1, w - 1]]
    border = corners[0]
    if not all(np.array_equal(border, c) for c in corners[1:]):
        return img_rgb  # no uniform border
    # tolerance match against border color
    diff = np.abs(arr.astype(int) - border.astype(int)).max(axis=2)
    nonborder = diff > 8
    if not nonborder.any():
        return img_rgb
    rows = np.where(nonborder.any(axis=1))[0]
    cols = np.where(nonborder.any(axis=0))[0]
    top, bottom = rows[0], rows[-1] + 1
    left, right = cols[0], cols[-1] + 1
    if (top, left, bottom, right) == (0, 0, h, w):
        return img_rgb
    return img_rgb.crop((left, top, right, bottom))


def remove_background_borderflood(img_rgba):
    """Set alpha=0 for border-connected background-like components only.

    Background-like = near-neutral (low channel spread) AND bright.
    Interior highlights are preserved because only components touching the
    image border are cleared.
    """
    arr = np.array(img_rgba)
    rgb = arr[:, :, :3].astype(int)
    mx = rgb.max(axis=2)
    mn = rgb.min(axis=2)
    bg_like = ((mx - mn) <= NEUTRAL_MAX_SPREAD) & (mn >= BRIGHT_MIN_CHANNEL)

    labels, n = ndimage.label(bg_like)
    if n == 0:
        return img_rgba

    # find labels touching any border
    border_labels = set()
    border_labels.update(labels[0, :].tolist())
    border_labels.update(labels[-1, :].tolist())
    border_labels.update(labels[:, 0].tolist())
    border_labels.update(labels[:, -1].tolist())
    border_labels.discard(0)
    if not border_labels:
        return img_rgba

    clear = np.isin(labels, list(border_labels))
    arr[clear, 3] = 0
    return Image.fromarray(arr, "RGBA")


def alpha_bbox_crop(img_rgba):
    arr = np.array(img_rgba)
    alpha = arr[:, :, 3]
    ys, xs = np.where(alpha > 0)
    if len(xs) == 0:
        return img_rgba
    top, bottom = ys.min(), ys.max() + 1
    left, right = xs.min(), xs.max() + 1
    return img_rgba.crop((left, top, right, bottom))


def fit_centered(img_rgba, size, method):
    """Resize preserving aspect to fit within (size-2), center on size×size."""
    inner = max(1, size - 2)
    w, h = img_rgba.size
    scale = min(inner / w, inner / h)
    new_w = max(1, round(w * scale))
    new_h = max(1, round(h * scale))
    resized = img_rgba.resize((new_w, new_h), RESAMPLE[method])
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox = (size - new_w) // 2
    oy = (size - new_h) // 2
    canvas.paste(resized, (ox, oy))
    return canvas


def process_block(img, args):
    img = img.convert("RGB")
    img = apply_contrast(img, args.contrast)
    if args.crop:
        img = auto_border_crop(img)
    img = downscale(img, args.size, args.method)
    img = quantize_flat(img, args.colors)
    return img, "block"


def process_item(img, args):
    img = img.convert("RGBA")
    img = remove_background_borderflood(img)
    img = alpha_bbox_crop(img)
    img = fit_centered(img, args.size, args.method)

    # contrast on RGB only
    rgb = apply_contrast(img.convert("RGB"), args.contrast)

    # hard-threshold alpha for crisp edges
    alpha = np.array(img)[:, :, 3]
    hard_alpha = np.where(alpha >= 128, 255, 0).astype(np.uint8)

    # quantize RGB flat, then reattach hard alpha
    rgb = quantize_flat(rgb, args.colors)
    out = rgb.convert("RGBA")
    out_arr = np.array(out)
    out_arr[:, :, 3] = hard_alpha
    return Image.fromarray(out_arr, "RGBA"), "item"


def process_rescale_only(img, args):
    img = img.convert("RGBA") if "A" in img.getbands() else img.convert("RGB")
    img = apply_contrast(img, args.contrast) if img.mode == "RGB" else img
    img = downscale(img, args.size, args.method)
    return img, "rescale-only"


def pick_mode(args):
    if args.item:
        return process_item
    if args.rescale_only:
        return process_rescale_only
    return process_block  # default --block


def gather_files(args):
    names = sorted(os.listdir(INPUT_DIR))
    files = [n for n in names if n.lower().endswith(EXTS)]
    if args.file:
        if args.file not in files:
            print(f"--file {args.file!r} not found in input/ "
                  f"(have: {files or 'none'})", file=sys.stderr)
            return []
        files = [args.file]
    return files


def build_parser():
    p = argparse.ArgumentParser(
        prog="rescale.py",
        description="Finish AI-generated Minecraft textures: rescale, "
                    "quantize, and (item mode) cut transparent backgrounds. "
                    "Reads input/, writes Minecraft-safe PNGs to output/.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    mode = p.add_mutually_exclusive_group()
    mode.add_argument(
        "--block", action="store_true",
        help="MODE: opaque block texture (cables, machine faces, ores). "
             "Optional border crop, downscale, flat-palette quantize. "
             "RGB output, no transparency. [default mode]")
    mode.add_argument(
        "--item", action="store_true",
        help="MODE: item texture with transparent background (magnet, coil, "
             "ingot). Border-flood bg removal, alpha-bbox crop, centered fit, "
             "hard-threshold alpha, quantize with alpha preserved.")
    mode.add_argument(
        "--rescale-only", action="store_true",
        help="MODE: only downscale to --size, nothing else.")

    p.add_argument("--size", type=int, default=16,
                   help="target square size in pixels")
    p.add_argument("--colors", type=int, default=8,
                   help="flat-palette colors, no dithering (0 disables)")
    p.add_argument("--method", choices=list(RESAMPLE), default="nearest",
                   help="resample filter: nearest=crisp pixel art; "
                        "box/lanczos=fallbacks for noisy sources")
    p.add_argument("--contrast", type=float, default=1.0,
                   help="contrast multiplier (1.0 = unchanged)")
    p.add_argument("--crop", action="store_true",
                   help="(block mode) trim a uniform border/frame if present")
    p.add_argument("--file", metavar="NAME",
                   help="process only this one file in input/")
    return p


def main():
    args = build_parser().parse_args()

    os.makedirs(INPUT_DIR, exist_ok=True)
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    processor = pick_mode(args)
    files = gather_files(args)
    if not files:
        print(f"No images in {INPUT_DIR} "
              f"(extensions: {', '.join(EXTS)}).")
        return

    for name in files:
        src = os.path.join(INPUT_DIR, name)
        try:
            img = Image.open(src)
        except Exception as e:
            print(f"{name} -> SKIP (cannot open: {e})", file=sys.stderr)
            continue

        result, mode = processor(img, args)
        out_name = sanitize_name(name)
        dst = os.path.join(OUTPUT_DIR, out_name)
        result.save(dst)
        w, h = result.size
        print(f"{name} -> {out_name}  |  {w}x{h}  |  mode={mode}")


if __name__ == "__main__":
    main()
