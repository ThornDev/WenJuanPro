#!/usr/bin/env python3
"""Generate sample image assets for WenJuanPro TXT config demo."""

import os
import math
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "assets")
os.makedirs(OUT, exist_ok=True)

# Try to find a font that supports Chinese
FONT_PATHS = [
    "/System/Library/Fonts/PingFang.ttc",
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Hiragino Sans GB.ttc",
    "/Library/Fonts/Arial Unicode.ttf",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
]
FONT_PATH = None
for p in FONT_PATHS:
    if os.path.exists(p):
        FONT_PATH = p
        break


def get_font(size):
    if FONT_PATH:
        try:
            return ImageFont.truetype(FONT_PATH, size)
        except Exception:
            pass
    return ImageFont.load_default()


def save(img, name):
    path = os.path.join(OUT, name)
    img.save(path)
    print(f"  ✅ {name} ({img.width}x{img.height})")


# ──────────────────────────────────────────────
# 1. 题干图片 (宽 800px, 高按内容)
# ──────────────────────────────────────────────

def stem_image(w, h, bg, draw_fn, name):
    img = Image.new("RGB", (w, h), bg)
    draw = ImageDraw.Draw(img)
    draw_fn(draw, w, h)
    save(img, name)


# Q4: 动物照片题干
def draw_animal(draw, w, h):
    f = get_font(28)
    fs = get_font(18)
    # Draw a simple cat silhouette
    draw.ellipse([300, 80, 500, 250], fill="#FF9800")  # body
    draw.ellipse([330, 40, 470, 150], fill="#FF9800")  # head
    draw.polygon([(340, 60), (360, 20), (380, 70)], fill="#FF9800")  # ear L
    draw.polygon([(420, 60), (440, 20), (460, 70)], fill="#FF9800")  # ear R
    draw.ellipse([365, 90, 385, 110], fill="white")  # eye L
    draw.ellipse([415, 90, 435, 110], fill="white")  # eye R
    draw.ellipse([370, 95, 380, 105], fill="black")
    draw.ellipse([420, 95, 430, 105], fill="black")
    draw.ellipse([390, 115, 410, 130], fill="#E65100")  # nose
    draw.text((w // 2, h - 30), "请判断这是什么动物？", fill="black", font=f, anchor="mm")


# Simpler approach - just use shapes and text
def make_stem_with_text(text, subtitle, name, bg="#FFFFFF", w=800, h=300):
    img = Image.new("RGB", (w, h), bg)
    draw = ImageDraw.Draw(img)
    f_big = get_font(32)
    f_small = get_font(20)
    draw.text((w // 2, h // 2 - 20), text, fill="black", font=f_big, anchor="mm")
    if subtitle:
        draw.text((w // 2, h // 2 + 30), subtitle, fill="#666666", font=f_small, anchor="mm")
    save(img, name)


def make_chart(name):
    """Temperature chart stem image."""
    w, h = 800, 400
    img = Image.new("RGB", (w, h), "white")
    draw = ImageDraw.Draw(img)
    f = get_font(16)
    ft = get_font(24)

    # Title
    draw.text((w // 2, 30), "某城市2024年月平均气温", fill="black", font=ft, anchor="mm")

    # Axes
    left, bottom, right, top = 80, 350, 750, 70
    draw.line([(left, bottom), (left, top)], fill="black", width=2)
    draw.line([(left, bottom), (right, bottom)], fill="black", width=2)

    months = ["1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"]
    temps = [2, 5, 12, 18, 24, 28, 33, 35, 29, 20, 12, 5]

    bar_w = (right - left) // 14
    for i, (m, t) in enumerate(zip(months, temps)):
        x = left + (i + 1) * (right - left) // 13
        bar_h = int((t / 40) * (bottom - top))
        color = "#FF5722" if t >= 30 else "#FF9800" if t >= 20 else "#2196F3" if t >= 10 else "#03A9F4"
        draw.rectangle([x - bar_w//2, bottom - bar_h, x + bar_w//2, bottom], fill=color)
        draw.text((x, bottom + 15), m, fill="black", font=f, anchor="mm")
        draw.text((x, bottom - bar_h - 12), f"{t}°", fill="black", font=f, anchor="mm")

    draw.text((30, (top + bottom) // 2), "°C", fill="black", font=f, anchor="mm")
    save(img, name)


def make_pattern_sequence(name):
    """Pattern sequence stem: circle → square → triangle → ?"""
    w, h = 800, 250
    img = Image.new("RGB", (w, h), "white")
    draw = ImageDraw.Draw(img)
    f = get_font(24)
    ft = get_font(40)

    draw.text((w // 2, 30), "找出图形规律", fill="black", font=f, anchor="mm")

    # Shapes
    shapes_x = [120, 280, 440, 600]
    colors = ["#2196F3", "#4CAF50", "#FF9800", None]
    labels = ["", "", "", "?"]

    # Circle
    draw.ellipse([80, 80, 160, 160], fill=colors[0], outline="black", width=2)
    # Square
    draw.rectangle([240, 80, 320, 160], fill=colors[1], outline="black", width=2)
    # Triangle
    draw.polygon([(400, 160), (440, 80), (480, 160)], fill=colors[2], outline="black", width=2)
    # Question mark
    draw.rectangle([560, 80, 640, 160], outline="#999999", width=3)
    draw.text((600, 120), "?", fill="#999999", font=ft, anchor="mm")

    # Arrows
    for x in [190, 350, 510]:
        draw.text((x, 120), "→", fill="black", font=ft, anchor="mm")

    save(img, name)


def make_shape(name, shape_type, color, label, size=200):
    """Single shape option image."""
    img = Image.new("RGB", (size, size), "white")
    draw = ImageDraw.Draw(img)
    f = get_font(16)
    m = 20  # margin

    if shape_type == "circle":
        draw.ellipse([m, m, size-m, size-m], fill=color, outline="black", width=2)
    elif shape_type == "square":
        draw.rectangle([m, m, size-m, size-m], fill=color, outline="black", width=2)
    elif shape_type == "triangle":
        cx = size // 2
        draw.polygon([(cx, m), (size-m, size-m), (m, size-m)], fill=color, outline="black", width=2)
    elif shape_type == "diamond":
        cx, cy = size//2, size//2
        r = size//2 - m
        draw.polygon([(cx, cy-r), (cx+r, cy), (cx, cy+r), (cx-r, cy)], fill=color, outline="black", width=2)
    elif shape_type == "star":
        cx, cy, r = size//2, size//2, size//2 - m
        points = []
        for i in range(10):
            a = math.radians(i * 36 - 90)
            ri = r if i % 2 == 0 else r * 0.4
            points.append((cx + ri * math.cos(a), cy + ri * math.sin(a)))
        draw.polygon(points, fill=color, outline="black", width=2)

    if label:
        draw.text((size//2, size - 10), label, fill="black", font=f, anchor="mm")
    save(img, name)


def make_animal_icon(name, animal, color):
    """Simple animal icon for option."""
    size = 200
    img = Image.new("RGB", (size, size), "white")
    draw = ImageDraw.Draw(img)
    f = get_font(60)
    fl = get_font(16)
    # Use text emoji-style representation
    symbols = {"cat": "🐱", "dog": "🐶", "bird": "🐦", "fish": "🐟",
               "penguin": "🐧", "eagle": "🦅", "dolphin": "🐬", "tiger": "🐯"}
    # Draw colored circle background
    draw.ellipse([20, 10, 180, 170], fill=color)
    # Draw animal name in Chinese
    names = {"cat": "猫", "dog": "狗", "bird": "鸟", "fish": "鱼",
             "penguin": "企鹅", "eagle": "鹰", "dolphin": "海豚", "tiger": "虎"}
    draw.text((size//2, 90), names.get(animal, "?"), fill="white", font=f, anchor="mm")
    draw.text((size//2, 185), animal, fill="#666", font=fl, anchor="mm")
    save(img, name)


def make_fruit(name, fruit_name, color):
    """Simple fruit image."""
    size = 200
    img = Image.new("RGB", (size, size), "white")
    draw = ImageDraw.Draw(img)
    f = get_font(48)
    fl = get_font(16)
    draw.ellipse([30, 20, 170, 160], fill=color)
    draw.text((size//2, 90), fruit_name, fill="white", font=f, anchor="mm")
    draw.text((size//2, 180), fruit_name, fill="#333", font=fl, anchor="mm")
    save(img, name)


def make_color_object(name, label, obj_color, bg="white"):
    """Colored object for color identification questions."""
    size = 200
    img = Image.new("RGB", (size, size), bg)
    draw = ImageDraw.Draw(img)
    f = get_font(18)
    # Draw object
    draw.rounded_rectangle([30, 30, 170, 150], radius=15, fill=obj_color)
    draw.text((size//2, 175), label, fill="black", font=f, anchor="mm")
    save(img, name)


def make_before_after(name, shape1_type, shape2_type, color):
    """Before/after comparison image."""
    w, h = 400, 250
    img = Image.new("RGB", (w, h), "white")
    draw = ImageDraw.Draw(img)
    f = get_font(20)

    if shape1_type == "circle":
        draw.ellipse([50, 50, 150, 150], fill=color, outline="black", width=2)
    elif shape1_type == "square":
        draw.rectangle([50, 50, 150, 150], fill=color, outline="black", width=2)

    draw.text((200, 100), "→", fill="black", font=get_font(40), anchor="mm")

    if shape2_type == "circle":
        draw.ellipse([250, 50, 350, 150], fill=color, outline="black", width=2)
    elif shape2_type == "triangle":
        draw.polygon([(250, 150), (300, 50), (350, 150)], fill=color, outline="black", width=2)
    elif shape2_type == "star":
        cx, cy, r = 300, 100, 50
        points = []
        for i in range(10):
            a = math.radians(i * 36 - 90)
            ri = r if i % 2 == 0 else r * 0.4
            points.append((cx + ri * math.cos(a), cy + ri * math.sin(a)))
        draw.polygon(points, fill=color, outline="black", width=2)

    draw.text((w//2, h - 20), name.replace(".png", ""), fill="#999", font=get_font(14), anchor="mm")
    save(img, name)


# ──────────────────────────────────────────────
# Generate all assets
# ──────────────────────────────────────────────

print("=== 生成题干图片 ===")

# Q4: animal photo
make_stem_with_text("🐱", "请判断这是什么动物？", "animal_photo.png", w=800, h=300)

# Temperature chart
make_chart("temp_chart.png")

# Pattern sequence
make_pattern_sequence("pattern_sequence.png")

# Before/After
make_before_after("before.png", "circle", "circle", "#2196F3")
make_before_after("after.png", "circle", "triangle", "#2196F3")

# Fruit question stem
make_stem_with_text("🍎 水果识别测试", "请观察图片选择正确答案", "fruit_question.png")

# Q4 stem (pure image question)
make_stem_with_text("观察下图", "以下哪个选项正确描述了图中内容？", "q4_photo.png")

# Pattern question stem
make_stem_with_text("图形推理", "请找出规律并选择正确答案", "q8_stem.png")

# Diagram for staged question
make_stem_with_text("📊 数据分析题", "请仔细观察图表后回答问题", "q9_diagram.png")


print("\n=== 生成选项图片 (图形) ===")

# Pattern answer options
make_shape("pattern_a.png", "square", "#4CAF50", "A", 200)
make_shape("pattern_b.png", "diamond", "#9C27B0", "B", 200)
make_shape("pattern_c.png", "star", "#F44336", "C", 200)
make_shape("pattern_d.png", "circle", "#2196F3", "D", 200)

# Shape identification (triangles vs others)
make_shape("shape_triangle1.png", "triangle", "#FF9800", "等边三角形", 200)
make_shape("shape_circle.png", "circle", "#2196F3", "圆形", 200)
make_shape("shape_triangle2.png", "triangle", "#4CAF50", "直角三角形", 200)
make_shape("shape_square.png", "square", "#9C27B0", "正方形", 200)

# Q8 options (pure image)
make_shape("q8_a.png", "circle", "#F44336", "A", 200)
make_shape("q8_b.png", "square", "#2196F3", "B", 200)
make_shape("q8_c.png", "triangle", "#4CAF50", "C", 200)
make_shape("q8_d.png", "star", "#FF9800", "D", 200)


print("\n=== 生成选项图片 (动物) ===")

make_animal_icon("cat_icon.png", "cat", "#FF9800")
make_animal_icon("dog_icon.png", "dog", "#795548")
make_animal_icon("bird_icon.png", "bird", "#03A9F4")
make_animal_icon("fish_icon.png", "fish", "#00BCD4")
make_animal_icon("penguin.png", "penguin", "#607D8B")
make_animal_icon("eagle.png", "eagle", "#8D6E63")
make_animal_icon("dolphin.png", "dolphin", "#0288D1")
make_animal_icon("tiger.png", "tiger", "#E65100")


print("\n=== 生成选项图片 (水果/蔬菜) ===")

make_fruit("apple.png", "苹果", "#F44336")
make_fruit("banana.png", "香蕉", "#FFC107")
make_fruit("grape.png", "葡萄", "#9C27B0")
make_fruit("carrot.png", "胡萝卜", "#FF9800")
make_fruit("potato.png", "土豆", "#795548")


print("\n=== 生成选项图片 (颜色物体) ===")

make_color_object("red_apple.png", "红苹果", "#F44336")
make_color_object("blue_sky.png", "蓝天", "#2196F3")
make_color_object("red_car.png", "红车", "#D32F2F")
make_color_object("green_tree.png", "绿树", "#4CAF50")


print(f"\n✅ 全部完成！共生成 {len(os.listdir(OUT))} 个图片文件")
print(f"📁 输出目录: {OUT}")
print(f"\n使用方式:")
print(f"  adb push {OUT}/ /sdcard/WenJuanPro/assets/")
