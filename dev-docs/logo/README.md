# ProjectForge logo

`master/` holds the logo as designed: three flames (cubic Bézier paths) and "ProjectForge" (Inter SemiBold, SIL Open
Font License, outlined to paths, so no font is needed), each as `horizontal` (transparent), `square` and `icon` (on a
light and on a dark plate). The `*.editable.svg` keep the text as text (Inter SemiBold must be installed) for later
changes; outline it again before rendering. Every logo asset of the repository is rendered from the outlined masters
by

```bash
python3 -I dev-docs/logo/render-logos.py   # needs rsvg-convert (librsvg) and magick (ImageMagick)
```

Never edit the rendered files by hand; change a master (or the script) and run it again. The `square` masters are
rendered nowhere; they are for use outside the repository (profile pictures, slides).

| Rendered                                                                               | From                          | Where                                   |
| -------------------------------------------------------------------------------------- | ----------------------------- | --------------------------------------- |
| `projectforge-next/components/shared/projectforge-logo-{light,dark}.svg`               | horizontal, no plate          | logo row of the web interface           |
| `site/assets/img/projectforge-logo.svg`                                                | horizontal light, no plate    | header and footer of the website        |
| `site/uploads/projectforge-logo.png` (1200 × 630)                                      | horizontal light, white       | preview image of the website (og:image) |
| `favicon.{ico,png}` (`site/`, `site/uploads/`, `projectforge-application/.../static/`) | flames, transparent           | browser tab                             |
| `apple-touch-icon*.png`, `icon-{192,512}.png` (`projectforge-application/.../static/`) | icon dark, full-bleed         | home screen, PWA (`manifest.json`)      |
| `projectforge-app/assets/icon.png`, `splash-icon.png`                                  | icon dark, full-bleed; flames | app icon (opaque), splash               |
| `projectforge-app/assets/adaptive-icon.png`                                            | flames at 50 %, transparent   | Android adaptive icon (foreground)      |
| `projectforge-business/src/main/resources/images/default-logo.png`                     | horizontal light, no plate    | Wallet pass of the business card (logo) |
| `projectforge-business/src/main/resources/images/default-icon.png`                     | flames, transparent           | Wallet pass of the business card (icon) |
