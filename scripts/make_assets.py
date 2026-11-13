"""Draws every texture of Barely Crafting by hand-placed pixels (no gradients, small flat palettes).

Writes into common/src/main/resources/assets/barelycrafting/textures: three 16x16 block textures and six 10x10 button icons.
Run:  python scripts/make_assets.py
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "common/src/main/resources/assets/barelycrafting/textures"


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# Wood palette (original, three tones plus seams).
WOOD_LIGHT = rgb("#c2a06a")
WOOD_MID = rgb("#a98350")
WOOD_DARK = rgb("#85633a")
WOOD_SEAM = rgb("#5e4527")
INLAY = rgb("#4a361e")
INLAY_HI = rgb("#d2b27c")
IRON = rgb("#9a9a9a")
IRON_HI = rgb("#d6d6d6")
IRON_LO = rgb("#5c5c5c")


def planks(img, x0, y0, w, h, seed, base=WOOD_MID):
    """Horizontal boards 4 pixels tall with a dark seam under each and a few lighter and darker specks."""
    rnd = random.Random(seed)
    for y in range(y0, y0 + h):
        board = (y - y0) // 4
        for x in range(x0, x0 + w):
            tone = base if board % 2 == 0 else WOOD_LIGHT if base == WOOD_MID else WOOD_MID
            img.putpixel((x, y), tone)
        if (y - y0) % 4 == 3:
            for x in range(x0, x0 + w):
                img.putpixel((x, y), WOOD_SEAM if base == WOOD_MID else WOOD_DARK)
    for _ in range(w * h // 18):
        x = rnd.randrange(x0, x0 + w)
        y = rnd.randrange(y0, y0 + h)
        if (y - y0) % 4 != 3:
            img.putpixel((x, y), rnd.choice([WOOD_LIGHT, WOOD_DARK]))


def border(img, color, inset=0):
    for i in range(inset, 16 - inset):
        for p in ((i, inset), (i, 15 - inset), (inset, i), (15 - inset, i)):
            img.putpixel(p, color)


def top():
    img = Image.new("RGBA", (16, 16))
    planks(img, 0, 0, 16, 16, 11)
    border(img, WOOD_SEAM)
    for r in range(3):
        for c in range(3):
            x = 1 + c * 5
            y = 1 + r * 5
            if x + 4 > 15 or y + 4 > 15:
                pass
            for dy in range(4):
                for dx in range(4):
                    img.putpixel((x + dx, y + dy), INLAY)
            for dx in range(4):
                img.putpixel((x + dx, y + 3), INLAY_HI)
            for dy in range(4):
                img.putpixel((x + 3, y + dy), INLAY_HI)
    return img


def side():
    img = Image.new("RGBA", (16, 16))
    planks(img, 0, 0, 16, 16, 23)
    border(img, WOOD_SEAM)
    # Iron plate with four rivets in the middle.
    for y in range(4, 12):
        for x in range(4, 12):
            img.putpixel((x, y), IRON)
    for i in range(4, 12):
        img.putpixel((i, 4), IRON_HI)
        img.putpixel((4, i), IRON_HI)
        img.putpixel((i, 11), IRON_LO)
        img.putpixel((11, i), IRON_LO)
    for p in ((5, 5), (10, 5), (5, 10), (10, 10)):
        img.putpixel(p, IRON_LO)
    for x in range(6, 10):
        img.putpixel((x, 7), IRON_LO)
        img.putpixel((x, 8), IRON_LO)
    return img


def bottom():
    img = Image.new("RGBA", (16, 16))
    planks(img, 0, 0, 16, 16, 37, base=WOOD_DARK)
    border(img, WOOD_SEAM)
    return img


# Button icons. '#' main color, 'a' accent color. Edges facing up-left get a light tone, down-right a dark one.
MAIN = rgb("#373737")
ICONS = {
    "clear": ("#373737", [
        "..........",
        ".aa....aa.",
        ".aaa..aaa.",
        "..aaaaaa..",
        "...aaaa...",
        "...aaaa...",
        "..aaaaaa..",
        ".aaa..aaa.",
        ".aa....aa.",
        "..........",
    ]),
    "rotate": ("#373737", [
        "...aaaa...",
        "..aa..aa..",
        ".aa....aa.",
        ".a......a.",
        ".a....aaaa",
        ".a.....aa.",
        ".aa.....a.",
        "..aa......",
        "...aaaa...",
        "..........",
    ]),
    "mirror": ("#373737", [
        "....##....",
        "a...  ...a",
        "aa..##..aa",
        "aaa.  .aaa",
        "aaaa##aaaa",
        "aaaa  aaaa",
        "aaa.##.aaa",
        "aa..  ..aa",
        "a...##...a",
        "....  ....",
    ]),
    "balance": ("#373737", [
        "..........",
        "..aa.aa.aa",
        "..aa.aa.aa",
        "..aa.aa.aa",
        "..aa.aa.aa",
        "..aa.aa.aa",
        "..aa.aa.aa",
        ".#########",
        ".#########",
        "..........",
    ]),
    "refill": ("#373737", [
        "....aa....",
        "....aa....",
        "....aa....",
        "..aaaaaa..",
        "...aaaa...",
        "....aa....",
        "..........",
        "#........#",
        "#........#",
        "##########",
    ]),
    "craft_all": ("#373737", [
        "..........",
        ".a...a....",
        ".aa..aa...",
        ".aaa.aaa..",
        ".aaaaaaaa.",
        ".aaaaaaaa.",
        ".aaa.aaa..",
        ".aa..aa...",
        ".a...a....",
        "..........",
    ]),
}


def shade(color, amount):
    """Moves a color toward white (positive) or black (negative) by a fixed step."""
    r, g, b, a = color
    if amount > 0:
        return (min(255, r + amount), min(255, g + amount), min(255, b + amount), a)
    return (max(0, r + amount), max(0, g + amount), max(0, b + amount), a)


def icon(accent_hex, rows, main=None):
    """Flat two-tone glyph: the accent color with a darker pixel on every lower or right edge (no light edge, so it stays crisp)."""
    accent = rgb(accent_hex)
    img = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
    cells = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in "#a":
                cells[(x, y)] = (main or MAIN) if ch == "#" else accent
    for (x, y), c in cells.items():
        edge = (x, y + 1) not in cells or (x + 1, y) not in cells
        img.putpixel((x, y), shade(c, -45) if edge else c)
    return img


def main():
    (TEX / "block").mkdir(parents=True, exist_ok=True)
    (TEX / "gui/icon").mkdir(parents=True, exist_ok=True)
    top().save(TEX / "block/crafting_station_top.png")
    side().save(TEX / "block/crafting_station_side.png")
    bottom().save(TEX / "block/crafting_station_bottom.png")
    for name, (accent, rows) in ICONS.items():
        icon(accent, rows).save(TEX / f"gui/icon/{name}.png")
        icon("#e6e3f7", rows, rgb("#e6e3f7")).save(TEX / f"gui/icon/{name}_light.png")
    # A preview sheet outside the repo tree is made by scripts/preview_assets.py when needed.
    print("textures written to", TEX)


if __name__ == "__main__":
    main()
