#!/usr/bin/env python3
"""
Clean the magenta ("purple") keying out of ocean_spritesheet_purple.png and
slice it into a 32x32 tileset.

The source sheet is 16 columns x 7 art rows of ~100x100 px cells separated by
~8px magenta borders. Row 7 (the detail row) draws thin sprites on a solid
magenta BACKGROUND, so magenta must be removed from inside cells, not just the
borders.

Outputs (under resources/images/objects/ocean/):
  - ocean_tileset.png            packed 16x7 grid of clean 32x32 tiles (magenta -> transparent)
  - seabed/  shallow/  medium/  deep/  details/   individual 32x32 PNGs

Run:  python3 tools/clean_ocean_spritesheet.py
"""
import os
from PIL import Image
import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "resources/images/objects/_spritesheets/ocean_spritesheet_purple.png")
OUT_DIR = os.path.join(ROOT, "resources/images/objects/ocean")
TILE = 32  # target in-game tile size

# Art-cell boundaries measured from the source (between magenta separator bands).
# (start, end) inclusive pixel coords. 16 columns, 7 rows.
COLS = [(10,110),(119,219),(228,329),(338,440),(449,550),(559,660),(669,770),
        (779,881),(890,992),(1001,1102),(1111,1212),(1221,1322),(1332,1433),
        (1442,1543),(1552,1654),(1663,1763)]
# 7 tile rows (~100px each) plus the detail band at the bottom. The detail
# sprites sit on a SOLID magenta background, so that band reads as "all magenta"
# to a gap-detector and must be listed explicitly.
ROWS = [(12,112),(121,220),(229,329),(338,438),(447,547),(557,657),(666,765),
        (786,876)]  # last entry = detail band

# Semantic name per row band (0-indexed). 8 bands = spec rows 0..7.
ROW_GROUPS = {
    0: "seabed",   # light sand, rippled sand, pebbles, shells, algae, ...
    1: "seabed",   # mud, rocks, darker deep-transition ground
    2: "shallow",  # shallow transparent overlay frames
    3: "shallow",
    4: "medium",   # medium-depth overlay frames
    5: "deep",     # deep water full frames
    6: "deep",
    7: "details",  # foam edges/corners, bubbles, sparkles, ripples, drift
}


def is_magenta(arr):
    """Boolean mask of magenta-keyed pixels. Robust to the gradient in the key
    (R high, G low, B high, and R/B clearly dominate G)."""
    r = arr[:, :, 0].astype(int)
    g = arr[:, :, 1].astype(int)
    b = arr[:, :, 2].astype(int)
    return (r > 130) & (b > 130) & (g < 90) & (r - g > 70) & (b - g > 70)


def clean_cell(cell):
    """Strip magenta -> fully transparent, with a 1px anti-halo erosion so the
    feathered magenta edge around sprites doesn't leave purple fringe."""
    a = np.array(cell.convert("RGBA"))
    mask = is_magenta(a)
    # Erode the *kept* region by one pixel along magenta borders: any pixel
    # adjacent to magenta that is itself borderline-magenta also gets cleared.
    # Cheap dilation of the mask by 1px to swallow the feathered fringe.
    m = mask.copy()
    m[1:, :] |= mask[:-1, :]
    m[:-1, :] |= mask[1:, :]
    m[:, 1:] |= mask[:, :-1]
    m[:, :-1] |= mask[:, 1:]
    # Only extend into pixels that are at least "leaning magenta" (avoid eating
    # legitimate art that merely touches a border).
    r = a[:, :, 0].astype(int); g = a[:, :, 1].astype(int); b = a[:, :, 2].astype(int)
    leaning = (r - g > 30) & (b - g > 30) & (g < 130)
    final = mask | (m & leaning)
    a[final] = [0, 0, 0, 0]
    return Image.fromarray(a, "RGBA")


def fit_into(cell, size):
    """Trim transparent margins, then scale to fit a size x size box keeping
    aspect ratio, centered on a transparent canvas."""
    bbox = cell.getbbox()
    if bbox:
        cell = cell.crop(bbox)
    w, h = cell.size
    if w == 0 or h == 0:
        return Image.new("RGBA", (size, size), (0, 0, 0, 0))
    scale = min(size / w, size / h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    cell = cell.resize((nw, nh), Image.NEAREST)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.paste(cell, ((size - nw) // 2, (size - nh) // 2))
    return canvas


def main():
    src = Image.open(SRC).convert("RGBA")
    ncols, nrows = len(COLS), len(ROWS)

    for sub in set(ROW_GROUPS.values()):
        os.makedirs(os.path.join(OUT_DIR, sub), exist_ok=True)

    sheet = Image.new("RGBA", (ncols * TILE, nrows * TILE), (0, 0, 0, 0))
    counts = {}

    for ri, (y0, y1) in enumerate(ROWS):
        group = ROW_GROUPS[ri]
        for ci, (x0, x1) in enumerate(COLS):
            cell = src.crop((x0, y0, x1 + 1, y1 + 1))
            cell = clean_cell(cell)
            if group == "details":
                # Detail sprites are non-square; fit into 32x32 keeping aspect
                # ratio and pad transparent so foam/sparkle shapes aren't squashed.
                cell = fit_into(cell, TILE)
            else:
                cell = cell.resize((TILE, TILE), Image.NEAREST)
            sheet.paste(cell, (ci * TILE, ri * TILE))
            idx = counts.get(group, 0)
            counts[group] = idx + 1
            cell.save(os.path.join(OUT_DIR, group, f"{group}_{idx:02d}.png"))

    sheet_path = os.path.join(OUT_DIR, "ocean_tileset.png")
    sheet.save(sheet_path)

    # --- Verification: assert NO magenta survives anywhere in the outputs. ---
    out = np.array(Image.open(sheet_path).convert("RGBA"))
    remaining = int(is_magenta(out).sum())
    # A surviving magenta pixel that is also opaque is a real failure.
    opaque_magenta = int((is_magenta(out) & (out[:, :, 3] > 10)).sum())

    print(f"Wrote tileset: {sheet_path}  ({ncols*TILE}x{nrows*TILE})")
    for k, v in sorted(counts.items()):
        print(f"  {k:8s}: {v} tiles")
    print(f"Magenta-classified pixels remaining (any alpha): {remaining}")
    print(f"Magenta pixels that are still OPAQUE (must be 0): {opaque_magenta}")
    if opaque_magenta == 0:
        print("OK: all purple removed.")
    else:
        raise SystemExit(f"FAIL: {opaque_magenta} opaque magenta pixels remain")


if __name__ == "__main__":
    main()
