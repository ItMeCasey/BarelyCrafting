"""Draws the Barely Crafting icon (32x32 pixel art on the studio's night tile) (the banners come from branding/source/render_banners.py, the same kit as SMWTI).

Run from the repository root:  python scripts/make_branding.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg, common/src/main/resources/barelycrafting_logo.png (the mod list
logo), .

The picture: the station's 3x3 grid in gold, drawn on a night tile, with the chest it crafts from beside it.
All art is original. The pixel font is Silkscreen (SIL Open Font License, see licenses/OFL-silkscreen.txt).
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "branding/icon"
BANNERS = ROOT / "branding/banners"
FONT = ROOT / "branding/source/fonts/silkscreen.ttf"
N = 32

NIGHT = (15, 14, 26)
NIGHT2 = (26, 24, 48)
EDGE = (46, 42, 70)
EDGE_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
TEXT = (244, 242, 255)
MUTED = (169, 164, 200)
WOOD = (169, 131, 80)
WOOD_HI = (194, 160, 106)
WOOD_LO = (94, 69, 39)
CELL = (11, 10, 20)
BLUE = (122, 168, 255)

px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


def rect(x0, y0, x1, y1, c):
    for yy in range(y0, y1 + 1):
        for xx in range(x0, x1 + 1):
            put(xx, yy, c)


def tile():
    r = 4
    for y in range(N):
        for x in range(N):
            cx = min(max(x, r - 0.5), N - r - 0.5)
            cy = min(max(y, r - 0.5), N - r - 0.5)
            if math.hypot(x - cx, y - cy) <= r:
                px[y][x] = NIGHT2
    for y in range(N):
        for x in range(N):
            if px[y][x] is None:
                continue
            edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                px[y][x] = EDGE_TOP if y < N // 2 else EDGE


def draw_icon():
    """An isometric Crafting Station: the block's own top and side textures projected on a cube, on the night tile."""
    tile()
    tex = ROOT / "common/src/main/resources/assets/barelycrafting/textures/block"
    top = Image.open(tex / "crafting_station_top.png").convert("RGBA")
    side = Image.open(tex / "crafting_station_side.png").convert("RGBA")
    # A simplified 10x10 top (frame, three by three dark cells, a gold pickaxe pattern) reads better at 32 pixels than the 16x16 one.
    top = Image.new("RGBA", (10, 10), WOOD_HI + (255,))
    for i in range(10):
        for e in ((i, 0), (i, 9), (0, i), (9, i)):
            top.putpixel(e, WOOD_LO + (255,))
    for r in range(3):
        for c in range(3):
            for dy in range(2):
                for dx in range(2):
                    top.putpixel((1 + c * 3 + dx, 1 + r * 3 + dy), CELL + (255,))
            if r == 0:
                top.putpixel((1 + c * 3, 1), GOLD + (255,))
                top.putpixel((2 + c * 3, 1), GOLD + (255,))
            elif c == 1:
                top.putpixel((4, 1 + r * 3), GOLD_LO + (255,))
    L, T, R, B, H = (3, 11), (16, 4), (29, 11), (16, 18), 12
    ox, oy = 0, 0

    def shade(c, f):
        return (int(c[0] * f), int(c[1] * f), int(c[2] * f))

    cube = {}
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - L[0], y + 0.5 - L[1]
            a = (dx / 13 + dy / 7) / 2
            b = (dx / 13 - dy / 7) / 2
            if 0 <= a < 1 and 0 <= b < 1:
                cube[(x, y)] = shade(top.getpixel((min(9, int(a * 10)), min(9, int((1 - b) * 10)))), 1.0)
                continue
            if 0 <= x + 0.5 - L[0] < 13:
                a = (x + 0.5 - L[0]) / 13
                c = (y + 0.5 - L[1] - a * 7) / H
                if 0 <= c < 1:
                    cube[(x, y)] = shade(side.getpixel((int(a * 16), int(c * 16))), 0.78)
                    continue
            if 0 <= x + 0.5 - B[0] < 13:
                a = (x + 0.5 - B[0]) / 13
                c = (y + 0.5 - B[1] + a * 7) / H
                if 0 <= c < 1:
                    cube[(x, y)] = shade(side.getpixel((int(a * 16), int(c * 16))), 0.58)
    outline = (30, 20, 10)
    for (x, y), col in cube.items():
        px[y][x] = col
    for (x, y) in list(cube):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in cube and 0 <= x + dx < N and 0 <= y + dy < N:
                put(x + dx, y + dy, outline)
    # A soft shadow line under the block, like the studio tiles' gold rule.
    rect(6, 29, 25, 29, GOLD_LO)


def save_icons():
    ICON.mkdir(parents=True, exist_ok=True)
    base = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    for y in range(N):
        for x in range(N):
            if px[y][x] is not None:
                base.putpixel((x, y), px[y][x] + (255,))
    for size in (32, 64, 128, 256, 512, 1024):
        base.resize((size, size), Image.NEAREST).save(ICON / f"icon_{size}.png")
    base.resize((256, 256), Image.NEAREST).save(ROOT / "common/src/main/resources/barelycrafting_logo.png")
    rects = []
    for y in range(N):
        for x in range(N):
            if px[y][x] is not None:
                r, g, b = px[y][x]
                rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="#{r:02x}{g:02x}{b:02x}"/>')
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n"
    (ICON / "icon.svg").write_text(svg, encoding="utf-8")
    return base


def main():
    draw_icon()
    save_icons()
    print("icons written; run branding/source/render_banners.py for the banners")


if __name__ == "__main__":
    main()
