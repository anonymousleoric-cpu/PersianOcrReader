"""Generate launcher icons for MatnKhan (Persian OCR Reader)."""
from PIL import Image, ImageDraw

TEAL = (15, 118, 110, 255)        # 0F766E
WHITE = (255, 255, 255, 255)
AMBER = (245, 158, 11, 255)       # F59E0B

RES = "/home/z/my-project/android-app/PersianOcrReader/app/src/main/res"
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def rounded(draw, box, radius, fill):
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def make_base(round_mask: bool) -> Image.Image:
    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if round_mask:
        d.ellipse((6, 6, 506, 506), fill=TEAL)
    else:
        rounded(d, (6, 6, 506, 506), 112, TEAL)
    # document sheet
    rounded(d, (166, 122, 346, 398), 26, WHITE)
    # text lines
    for y in (186, 242, 298):
        rounded(d, (204, y, 308, y + 22), 11, TEAL)
    # amber scan bar
    rounded(d, (204, 336, 308, 362), 13, AMBER)
    return img


def main():
    for folder, size in SIZES.items():
        square = make_base(False).resize((size, size), Image.LANCZOS)
        circle = make_base(True).resize((size, size), Image.LANCZOS)
        square.save(f"{RES}/{folder}/ic_launcher.png")
        circle.save(f"{RES}/{folder}/ic_launcher_round.png")
    make_base(False).resize((192, 192), Image.LANCZOS).save("/home/z/my-project/public/icon.png")
    print("icons generated")


if __name__ == "__main__":
    main()
