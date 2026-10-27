# Quick color palette applying script

import os
from PIL import Image

PALETTE_FILE = "palette.png"
DYE_COLORS = [
    "black",
    "blue",
    "brown",
    "cyan",
    "gray",
    "green",
    "light_blue",
    "light_gray",
    "lime",
    "magenta",
    "orange",
    "pink",
    "purple",
    "red",
    "white",
    "yellow",
]

palette_image = Image.open(PALETTE_FILE).convert("RGBA")
palette_pixels = list(palette_image.getdata())
width, height = palette_image.size

# Row 0 = source colors
source_colors = palette_pixels[:width]

# Rows 1..16 = target colors
target_rows = [palette_pixels[row * width : (row + 1) * width] for row in range(1, height)]

# ---- Process images ----
for filename in os.listdir("."):
    if not filename.lower().endswith(".png") or filename == PALETTE_FILE:
        continue

    image = Image.open(filename).convert("RGBA")
    pixels = list(image.getdata())

    # Build a mapping dict from source -> target for each dye
    for color_index, color_name in enumerate(DYE_COLORS):
        target_colors = target_rows[color_index]  # Each row corresponds to a dye.

        color_map = {source: target for source, target in zip(source_colors, target_colors)}

        # Apply color mapping
        new_pixels = [color_map.get(pixel, pixel) for pixel in pixels]  # Keep unmatched pixels.
        recolored_image = Image.new("RGBA", image.size)
        recolored_image.putdata(new_pixels)

        # Save with new filename
        new_filename = "output/" + filename.replace("blue", color_name)
        recolored_image.save(new_filename)
        print(f"Saved {new_filename}")
