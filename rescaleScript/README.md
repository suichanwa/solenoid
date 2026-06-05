# rescaleScript

Finish AI-generated Minecraft textures: rescale, quantize, and (item mode) cut
transparent backgrounds. Drop raw images in `input/`, run, collect finished
PNGs from `output/`.

## Install

```
pip install Pillow numpy scipy
```

## Usage

```
python rescale.py [MODE] [OPTIONS]
```

Drop `.png/.jpg/.jpeg/.webp/.bmp` files into `input/`. Output names are
lowercased, sanitized (any char not in `[a-z0-9_]` -> `_`), and forced to
`.png`. `input/` and `output/` are auto-created.

### Modes (mutually exclusive)

- `--block` (default) — opaque block texture (cables, machine faces, ores).
  Optional border crop (`--crop`), downscale, flat-palette quantize. RGB
  output, **no** transparency.
- `--item` — item texture needing a transparent background (magnet, coil,
  ingot). Border-flood background removal (does **not** erase interior
  highlights), alpha-bbox crop, centered fit on a transparent canvas,
  hard-threshold alpha for crisp edges, quantize with alpha preserved.
- `--rescale-only` — only downscale to `--size`, nothing else.

### Options

| Option        | Default   | Meaning |
|---------------|-----------|---------|
| `--size N`    | `16`      | target square size (px) |
| `--colors N`  | `8`       | flat-palette colors, no dither (`0` disables) |
| `--method M`  | `nearest` | `nearest` (crisp pixel art) / `box` / `lanczos` |
| `--contrast F`| `1.0`     | contrast multiplier |
| `--crop`      | off       | (block mode) trim uniform border/frame |
| `--file NAME` | all       | process only this one file in `input/` |

Full help: `python rescale.py --help`

## Examples

```
# block texture, default 16x16, 8 colors
python rescale.py --block

# item with transparent bg, 32px, 12 colors
python rescale.py --item --size 32 --colors 12

# just resize a single noisy source with lanczos
python rescale.py --rescale-only --method lanczos --file render.png
```
