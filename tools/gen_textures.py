#!/usr/bin/env python3
"""Generates all 16x16 textures + the mod icon with no dependencies except the stdlib.
Run from the project root:  python3 tools/gen_textures.py
"""
import math, os, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "doomsday")

def write_png(path, w, h, px):
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(png)

class Img:
    def __init__(self, w=16, h=16, bg=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.p = [[bg for _ in range(w)] for _ in range(h)]
    def put(self, x, y, c):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.p[y][x] = c if len(c) == 4 else (*c, 255)
    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, c)
    def line(self, x0, y0, x1, y1, c):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            self.put(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)
    def outline(self, c=(18, 14, 24, 255)):
        src = [row[:] for row in self.p]
        for y in range(self.h):
            for x in range(self.w):
                if src[y][x][3] == 0:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < self.w and 0 <= ny < self.h and src[ny][nx][3] > 0:
                            self.p[y][x] = c
                            break
    def save(self, rel):
        write_png(os.path.join(ROOT, rel), self.w, self.h, self.p)

def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

# ---------------------------------------------------------------- orbital strike designator
def orbital():
    im = Img()
    im.rect(5, 5, 10, 14, (70, 76, 90))           # body
    im.rect(5, 5, 5, 14, (96, 104, 122))          # left highlight
    im.rect(10, 5, 10, 14, (44, 48, 60))          # right shadow
    im.rect(6, 6, 9, 9, (20, 40, 50))             # screen
    for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        im.put(x, y, (70, 220, 255))              # target ring
    im.put(7, 7, (255, 60, 60)); im.put(8, 8, (255, 60, 60))  # lock
    im.rect(6, 11, 9, 12, (200, 30, 30))          # red button
    im.rect(6, 11, 9, 11, (255, 90, 80))
    im.line(8, 1, 8, 4, (170, 176, 190))          # antenna
    im.put(8, 0, (255, 70, 70)); im.put(8, 1, (255, 150, 120))
    im.line(11, 3, 13, 1, (140, 146, 160))
    im.outline()
    im.save("textures/item/orbital_strike_designator.png")

# ---------------------------------------------------------------- meteor caller
def meteor():
    im = Img()
    # trail towards the top right
    for i in range(0, 9):
        t = i / 8
        c = mix((255, 200, 60), (120, 40, 20), t)
        im.put(9 + i * 0.8, 6 - i * 0.7, c)
        im.put(10 + i * 0.8, 6 - i * 0.7, mix(c, (60, 20, 10), 0.4))
        im.put(9 + i * 0.8, 7 - i * 0.7, mix(c, (60, 20, 10), 0.3))
    cx, cy = 6.0, 9.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 4.4:
                t = d / 4.4
                c = mix((255, 245, 150), (210, 60, 15), t ** 1.2)
                im.put(x, y, c)
    for (x, y) in ((4, 8), (7, 11), (8, 8)):
        im.put(x, y, (110, 40, 20))               # craters
    im.put(3, 12, (255, 160, 40)); im.put(2, 14, (255, 100, 30)); im.put(12, 12, (255, 180, 60))
    im.outline((40, 12, 8, 255))
    im.save("textures/item/meteor_caller.png")

# ---------------------------------------------------------------- Thor's wrath (hammer)
def thor():
    im = Img()
    im.rect(2, 2, 13, 7, (150, 156, 170))         # head
    im.rect(2, 2, 13, 3, (200, 206, 220))
    im.rect(2, 7, 13, 7, (96, 100, 112))
    im.rect(2, 2, 3, 7, (120, 124, 138))
    im.rect(12, 2, 13, 7, (110, 114, 128))
    im.rect(7, 8, 8, 14, (122, 82, 42))           # handle
    im.rect(7, 8, 7, 14, (160, 112, 60))
    for y in (10, 12):
        im.rect(7, y, 8, y, (80, 52, 28))         # leather wrap
    im.rect(6, 14, 9, 15, (200, 180, 70))         # pommel
    bolt = [(8, 2), (7, 3), (8, 4), (6, 5), (7, 6), (6, 7)]
    for x, y in bolt:
        im.put(x, y, (255, 245, 110))
    im.put(9, 3, (180, 230, 255)); im.put(5, 4, (180, 230, 255)); im.put(10, 6, (180, 230, 255))
    im.outline()
    im.save("textures/item/thors_wrath.png")

# ---------------------------------------------------------------- singularity core
def singularity():
    im = Img()
    cx, cy = 7.5, 7.5
    ang = math.radians(-22)
    ca, sa = math.cos(ang), math.sin(ang)
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            rx = dx * ca + dy * sa
            ry = -dx * sa + dy * ca
            e = math.hypot(rx / 7.2, ry / 2.7)
            if 0.5 <= e <= 1.0:
                t = (e - 0.5) / 0.5
                c = mix((255, 230, 150), (150, 40, 230), t)
                im.put(x, y, c)
            d = math.hypot(dx, dy)
            if d <= 3.6 and (ry > 0 or d <= 3.6 and e < 0.5 or True):
                pass
    # black disk in front, with a faint violet rim
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 3.7:
                im.put(x, y, (4, 0, 8))
            elif d <= 4.4:
                p = im.p[y][x]
                if p[3]:
                    im.put(x, y, mix(p[:3], (60, 10, 90), 0.6))
    im.put(5, 5, (90, 30, 140)); im.put(10, 4, (255, 255, 255)); im.put(2, 11, (255, 255, 255))
    im.put(13, 11, (200, 160, 255))
    im.outline((10, 4, 20, 255))
    im.save("textures/item/singularity_core.png")

# ---------------------------------------------------------------- nuke block
def trefoil(im, cx, cy, scale, fg, bg=None):
    for y in range(im.h):
        for x in range(im.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy) / scale
            a = math.degrees(math.atan2(-dy, dx)) % 360
            on = False
            if r <= 0.24:
                on = True
            elif 0.42 <= r <= 1.0:
                for c in (90, 210, 330):
                    d = abs((a - c + 180) % 360 - 180)
                    if d <= 30:
                        on = True
            if on:
                im.put(x, y, fg)

def nuke_side():
    im = Img(16, 16, (66, 74, 52, 255))
    for i in range(16):                            # panel border
        for c in (0, 15):
            im.put(i, c, (34, 40, 28)); im.put(c, i, (34, 40, 28))
    for x in range(1, 15):                         # hazard stripes top + bottom
        for y in (1, 2, 13, 14):
            im.put(x, y, (240, 200, 30) if ((x + y) // 2) % 2 == 0 else (30, 28, 24))
    trefoil(im, 8, 8, 4.6, (245, 205, 30))
    for (x, y) in ((1, 5), (1, 10), (14, 5), (14, 10)):
        im.put(x, y, (150, 156, 120))              # rivets
    im.save("textures/block/nuke_bomb_side.png")

def nuke_top():
    im = Img(16, 16, (58, 64, 46, 255))
    for i in range(16):
        for c in (0, 15):
            im.put(i, c, (34, 40, 28)); im.put(c, i, (34, 40, 28))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 5.0 <= d <= 6.4:
                im.put(x, y, (240, 200, 30))
            elif d < 5.0 and ((x + y) % 4 == 0):
                im.put(x, y, (46, 52, 36))
            if d <= 1.6:
                im.put(x, y, (200, 30, 30))
    im.save("textures/block/nuke_bomb_top.png")

def icon():
    n = 128
    im = Img(n, n, (0, 0, 0, 255))
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - n / 2, y - n / 2) / (n * 0.72)
            im.put(x, y, mix((255, 150, 30), (40, 6, 6), min(1, d)))
    trefoil(im, n / 2, n / 2, 44, (20, 12, 8))
    im.save("icon.png")

if __name__ == "__main__":
    orbital(); meteor(); thor(); singularity(); nuke_side(); nuke_top(); icon()
    print("textures written to", os.path.normpath(ROOT))
