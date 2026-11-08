#!/usr/bin/env python3
"""
Renders every ProjectForge logo asset of the repository from the master SVGs in master/.

Run from anywhere: python3 -I dev-docs/logo/render-logos.py
Needs rsvg-convert (librsvg) and magick (ImageMagick) on the PATH.

The masters square and icon have a background plate (rounded rect), horizontal is transparent. Derived here:
- the wordmark (flames + "ProjectForge") without plate, cropped, light and dark text: next, website, Wallet pass;
- the flames alone without plate, on a square: favicons, splash, icon of the Wallet pass;
- the flames smaller on a transparent square, inside the safe zone: foreground of the Android adaptive icon;
- opaque icons with the dark plate full-bleed (no rounded corners, the platforms round themselves): app, touch, PWA.
"""
import re
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
MASTER = HERE / "master"

DARK_PLATE = "#111d30"

# Content boxes of the masters (inkscape --query-all), a little padding around.
# projectforge-horizontal-*.svg, 2100 x 520; no padding on the right, so the text ends flush where the logo is
# right-aligned (logo row of next).
WORDMARK_BOX = (80, 45, 1480, 464)
# The flames of projectforge-icon-*.svg (1024 x 1024), centred on a square: favicons and splash.
FLAME_SQUARE = (95, 91, 836, 836)
# Share of the canvas the flames take in the foreground of the Android adaptive icon: the launcher masks it (circle,
# squircle, ...), only the inner 66/108 are safe. 0.5 keeps the flame tips inside that circle.
ADAPTIVE_FLAMES = 0.5
# The whole content of projectforge-horizontal-*.svg, without padding: centred in the website preview image.
WORDMARK_CONTENT = (100.7, 65.15, 1458.5, 423.65)
# Preview image of the website (og:image): the format link previews show uncropped, the wordmark taking 75 % of its width.
PREVIEW_SIZE = (1200, 630)
PREVIEW_WORDMARK = 0.75


def around(box: tuple[int, int, int, int], fraction: float) -> tuple[float, float, float, float]:
    """A square around the centre of the box, so that the box takes the fraction of its side."""
    x, y, w, h = box
    side = max(w, h) / fraction
    return x + w / 2 - side / 2, y + h / 2 - side / 2, side, side


def read_master(name: str) -> str:
    return (MASTER / name).read_text(encoding="utf-8")


def without_plate(svg: str, box: tuple[float, float, float, float], title: str) -> str:
    """Removes the background rect (if any) and crops the viewBox to the content."""
    svg = re.sub(r"<rect [^>]*/>", "", svg, count=1)
    svg = re.sub(r"<title>[^<]*</title>", f"<title>{title}</title>", svg, count=1)
    x, y, w, h = box
    return re.sub(
        r'<svg [^>]*>',
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="{x} {y} {w} {h}">',
        svg,
        count=1,
    )


def preview(svg: str) -> str:
    """The wordmark centred on a white landscape canvas (og:image)."""
    x, y, w, h = WORDMARK_CONTENT
    width, height = PREVIEW_SIZE
    vw = w / PREVIEW_WORDMARK
    vh = vw * height / width
    vx, vy = x + w / 2 - vw / 2, y + h / 2 - vh / 2
    svg = re.sub(r"<title>[^<]*</title>", "<title>ProjectForge</title>", svg, count=1)
    return re.sub(
        r"<svg [^>]*>",
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="{vx} {vy} {vw} {vh}">'
        f'<rect x="{vx}" y="{vy}" width="{vw}" height="{vh}" fill="#ffffff"/>',
        svg,
        count=1,
    )


def full_bleed(svg: str) -> str:
    """The plate without its rounded corners, in the dark colour."""
    return re.sub(r'<rect ([^>]*?) rx="[^"]*" fill="[^"]*"/>', rf'<rect \1 fill="{DARK_PLATE}"/>', svg, count=1)


def write(path: str, text: str) -> Path:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")
    print(f"  {path}")
    return target


def png(svg: Path, path: str, width: int, height: int | None = None, opaque: bool = False) -> Path:
    target = ROOT / path
    args = ["rsvg-convert", "-w", str(width)]
    if height:
        args += ["-h", str(height)]
    subprocess.run(args + ["-o", str(target), str(svg)], check=True)
    # Strip the alpha channel where the platforms want an opaque image (iOS app icon), and the metadata.
    post = ["-alpha", "off"] if opaque else []
    subprocess.run(["magick", str(target), *post, "-strip", str(target)], check=True)
    print(f"  {path}")
    return target


def ico(svg: Path, path: str) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        sizes = []
        for size in (16, 32, 48):
            out = Path(tmp) / f"{size}.png"
            subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), "-o", str(out), str(svg)], check=True)
            sizes.append(str(out))
        subprocess.run(["magick", *sizes, str(ROOT / path)], check=True)
    print(f"  {path}")


def main() -> None:
    print("SVG:")
    wordmark_light = without_plate(read_master("projectforge-horizontal-light.svg"), WORDMARK_BOX, "ProjectForge")
    wordmark_dark = without_plate(read_master("projectforge-horizontal-dark.svg"), WORDMARK_BOX, "ProjectForge")
    write("projectforge-next/components/shared/projectforge-logo-light.svg", wordmark_light)
    write("projectforge-next/components/shared/projectforge-logo-dark.svg", wordmark_dark)
    site_logo = write("site/assets/img/projectforge-logo.svg", wordmark_light)

    with tempfile.TemporaryDirectory() as tmp:
        square_flame = Path(tmp) / "flame-square.svg"
        square_flame.write_text(
            without_plate(read_master("projectforge-icon-light.svg"), FLAME_SQUARE, "ProjectForge"),
            encoding="utf-8",
        )
        adaptive_flame = Path(tmp) / "flame-adaptive.svg"
        adaptive_flame.write_text(
            without_plate(read_master("projectforge-icon-dark.svg"), around(FLAME_SQUARE, ADAPTIVE_FLAMES), "ProjectForge"),
            encoding="utf-8",
        )
        icon = Path(tmp) / "icon.svg"
        icon.write_text(full_bleed(read_master("projectforge-icon-dark.svg")), encoding="utf-8")
        og = Path(tmp) / "og.svg"
        og.write_text(preview(read_master("projectforge-horizontal-light.svg")), encoding="utf-8")

        print("Favicons (flames, transparent):")
        for base in ("site", "site/uploads", "projectforge-application/src/main/resources/static"):
            ico(square_flame, f"{base}/favicon.ico")
            png(square_flame, f"{base}/favicon.png", 32, 32)

        print("Touch and PWA icons (dark, opaque):")
        static = "projectforge-application/src/main/resources/static"
        png(icon, f"{static}/apple-touch-icon.png", 180, 180, opaque=True)
        png(icon, f"{static}/apple-touch-icon-precomposed.png", 180, 180, opaque=True)
        png(icon, f"{static}/icon-192.png", 192, 192, opaque=True)
        png(icon, f"{static}/icon-512.png", 512, 512, opaque=True)

        print("App:")
        png(icon, "projectforge-app/assets/icon.png", 1024, 1024, opaque=True)
        png(square_flame, "projectforge-app/assets/splash-icon.png", 1024, 1024)
        # Background: android.adaptiveIcon.backgroundColor in app.config.ts (DARK_PLATE).
        png(adaptive_flame, "projectforge-app/assets/adaptive-icon.png", 1024, 1024)

        print("Website preview image (og:image):")
        png(og, "site/uploads/projectforge-logo.png", *PREVIEW_SIZE, opaque=True)

        print("Wallet pass icon (largest 174 x 174):")
        png(square_flame, "projectforge-business/src/main/resources/images/default-icon.png", 174, 174)

    print("Wallet pass logo (largest logo box 480 x 150):")
    png(site_logo, "projectforge-business/src/main/resources/images/default-logo.png", 480)


if __name__ == "__main__":
    main()
