"""Generates the 9 tier overlays of the compressed blocks (16x16 PNG, stdlib only).

Design: riveted frame that blends into the block texture instead of covering it. Every element is an
engraving: a translucent dark pixel with a translucent light highlight on its inner side, so it reads on
light and dark blocks alike. The frame grows with the tier (corner rivets, edge rivets, frame, inner lines)
and gets more opaque.

    python tools/generate_overlays.py
"""
import struct
import zlib
from pathlib import Path

W = 16
MAX_TIER = 9
OUT = Path(__file__).resolve().parent.parent / 'mods/resources/src/main/resources/assets/dotcraftresources/textures/block'

def dark(tier):
    return (0, 0, 0, 40 + 10 * tier)


def light(tier):
    return (255, 255, 255, 30 + 8 * tier)


CORNER_SIZE = {1: 1, 2: 1, 3: 2, 4: 2, 5: 2, 6: 3, 7: 3, 8: 3, 9: 3}


def pixels(tier):
    px = set()

    def put(x, y):
        if 0 <= x < W and 0 <= y < W:
            px.add((x, y))

    def square(offset, dotted=False):
        for a in range(offset, W - offset):
            if dotted and min(a, W - 1 - a) % 2:  # symmetric dots
                continue
            for p in ((a, offset), (a, W - 1 - offset), (offset, a), (W - 1 - offset, a)):
                put(*p)

    for i in range(W):
        for j in range(W):
            if i + j <= CORNER_SIZE[tier]:
                for x, y in ((i, j), (W - 1 - i, j), (i, W - 1 - j), (W - 1 - i, W - 1 - j)):
                    put(x, y)
    if tier >= 2:  # rivets in the middle of the edges
        for a in (7, 8):
            for p in ((a, 0), (a, W - 1), (0, a), (W - 1, a)):
                put(*p)
    if tier >= 4:  # more rivets on the edges
        for a in (3, 12):
            for p in ((a, 0), (a, W - 1), (0, a), (W - 1, a)):
                put(*p)
    if tier >= 5:
        square(0)
    if tier >= 7:
        square(2, dotted=True)
    if tier >= 8:
        square(2)
    if tier >= 9:
        square(4, dotted=True)
    return px


def png(rows):
    raw = b''.join(b'\x00' + b''.join(bytes(p) for p in row) for row in rows)

    def chunk(kind, data):
        body = kind + data
        return struct.pack('>I', len(data)) + body + struct.pack('>I', zlib.crc32(body) & 0xFFFFFFFF)

    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', W, W, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for tier in range(1, MAX_TIER + 1):
        px = pixels(tier)
        cells = {p: dark(tier) for p in px}
        for x, y in px:  # highlight on the side facing the tile center
            h = (x + (1 if x < W // 2 else -1), y + (1 if y < W // 2 else -1))
            if 0 <= h[0] < W and 0 <= h[1] < W and h not in px:
                cells[h] = light(tier)
        rows = [[cells.get((x, y), (0, 0, 0, 0)) for x in range(W)] for y in range(W)]
        (OUT / f'compressed_overlay_{tier}.png').write_bytes(png(rows))


if __name__ == '__main__':
    main()
