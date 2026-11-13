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
    """A pedestal Crafting Station seen at an angle: a stone foot, a tapering column and a wide wooden top with the 3x3 grid, on the night tile."""
    tile()

    def shade(c, f):
        return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)))

    def top_face(u, v):
        """The grid seen from above: a wooden frame round a three by three field of dark cells, gold studs in the corners."""
        e = min(u, v, 1 - u, 1 - v)
        if e < 0.06:
            return WOOD_LO
        if (u < 0.2 or u > 0.8) and (v < 0.2 or v > 0.8) and e < 0.14 and min(abs(u - 0.5), abs(v - 0.5)) > 0.36:
            return GOLD
        if e < 0.1:
            return WOOD_HI
        gu, gv = (u - 0.1) / 0.8, (v - 0.1) / 0.8
        if 0 <= gu < 1 and 0 <= gv < 1:
            fu, fv = (gu * 3) % 1, (gv * 3) % 1
            if 0.12 < fu < 0.88 and 0.12 < fv < 0.88:
                return CELL
            return WOOD
        return WOOD
    STONE, STONE_HI, STONE_LO = (122, 124, 140), (158, 160, 178), (84, 86, 104)

    def stone(u, v):
        return STONE_LO if 0.46 < v < 0.56 else (STONE_HI if v < 0.12 else STONE)

    def planks(u, v):
        return WOOD_HI if v < 0.2 else (WOOD_LO if v > 0.8 else WOOD)

    # box: center x, top-face center y, half width s, side height h, face painters
    boxes = [
        (16, 21.5, 11, 3, stone, stone),    # foot
        (16, 12.5, 6, 9, stone, stone),     # column
        (16, 9.5, 13, 3, planks, None),     # top slab
    ]
    owner = {}
    for n, (cx, yc, s, h, side, _) in enumerate(boxes):
        for y in range(N):
            for x in range(N):
                px_, py_ = x + 0.5, y + 0.5
                dx, dy = px_ - cx, py_ - yc
                col = None
                if abs(dx) / s + abs(dy) / (s / 2) <= 1:  # top face
                    u = (dx / s + dy / (s / 2)) / 2 + 0.5
                    v = (dx / s - dy / (s / 2)) / 2 + 0.5
                    if n == 2:
                        col = top_face(u, v)
                    else:
                        col = shade(STONE_HI, 0.95)
                else:
                    if -s <= dx < 0:
                        ytop = yc + (dx + s) * 0.5
                        v = (py_ - ytop) / h
                        if 0 <= v < 1:
                            col = shade(side((dx + s) / s, v), 0.82)
                    elif 0 <= dx < s:
                        ytop = yc + s / 2 - dx * 0.5
                        v = (py_ - ytop) / h
                        if 0 <= v < 1:
                            col = shade(side(dx / s, v), 0.6)
                if col is not None:
                    px[y][x] = col
                    owner[(x, y)] = n
    # A gold band where the column meets the top, and a dark outline round the whole shape.
    outline = (30, 20, 10)
    for (x, y), n in list(owner.items()):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in owner and 0 <= q[0] < N and 0 <= q[1] < N:
                put(q[0], q[1], outline)
            elif q in owner and owner[q] < n and n == 2 and dy == -1:
                put(x, y, shade(px[y][x], 0.85))
    rect(6, 30, 25, 30, GOLD_LO)


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
