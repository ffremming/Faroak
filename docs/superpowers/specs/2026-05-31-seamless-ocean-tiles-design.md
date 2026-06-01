# Seamless Ocean Tiles — Design

## Problem

The open ocean shows an obvious grid: a hard seam appears at every tile boundary.
The cause is that the water-surface sprites are not seamlessly tileable — the left
edge does not match the right edge (and top vs bottom), so when the same 32×32
sprite is repeated across the grid, each boundary is a visible discontinuity. The
sprites are drawn semi-transparent over a seabed layer, so an alpha discontinuity
at the edge reads as a grid just as much as a colour one.

## Goal

Make the water-surface sprites seamlessly tileable so the per-tile grid disappears,
with **no code changes** — only the PNG pixel data changes. Fully reversible.

## Scope (12 sprites)

`resources/images/tile/`:
- ocean0.png, ocean1.png, ocean2.png
- mediumWater0.png, mediumWater1.png, mediumWater2.png
- midWater0.png, midWater1.png, midWater2.png
- shallowWater0.png, shallowWater1.png, shallowWater2.png

All are 32×32 RGBA surface-water textures. Out of scope: seabed*, oceanDetail*,
foam/border (B1/C0) variants, and the large `warm_ocean*` images.

## Technique — wrap-around edge feathering

For each sprite, blend a margin band (~6px) along each edge toward the pixel that
will sit against it when tiled (the opposite edge), using a cosine falloff that is
strongest exactly at the boundary and fades to zero toward the interior:

1. Horizontal pass: for column `x` within the left/right margin, blend the pixel
   with its wrap partner `x ± width` using weight `w(x)`. This makes the left and
   right edges continuous.
2. Vertical pass: same along the top/bottom margin for rows.
3. Both RGB and alpha channels are feathered.

Weight at distance `d` (0 = on the edge) into the margin of size `m`:
`w = 0.5 * (1 + cos(pi * d / m)) * 0.5` — i.e. 0.5 contribution exactly at the seam
(true average of the two sides → they meet) decaying to 0 at the inner margin edge,
so interior detail is untouched.

### Why not full offset-and-blend

Rolling the image by half and blending the resulting cross would smear the interior
wave lines across the whole sprite. Edge-feathering touches only the mismatched
borders, which is exactly what produces the grid, and preserves the sprite's centre.

## Safety / reversibility

- Originals copied to `resources/images/tile/_grid_backup/` before any write.
- A 3×3 tiled preview is rendered for each sprite (before and after) and inspected
  to confirm the seam is gone and the texture still looks like water.

## Verification

Visual: 3×3 tiled montage per sprite shows no boundary lines after processing.
No build/test impact — the loader reads the same filenames at the same 32×32 size.
