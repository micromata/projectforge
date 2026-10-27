// A dot on the favicon of the tab, e.g. while a confidential conversation is offered. The icon is drawn into a
// canvas and set as data: url (allowed by img-src of the Content Security Policy); removing the badge restores the
// original href.

const SIZE = 32;

let original: string | null = null;
let badged: string | null = null;

function iconLink(): HTMLLinkElement | null {
  return document.querySelector<HTMLLinkElement>('link[rel="icon"]');
}

/** Shows or removes the dot. */
export async function setFaviconBadge(on: boolean): Promise<void> {
  const link = iconLink();
  if (!link) return;
  if (!on) {
    if (original != null) link.href = original;
    original = null;
    return;
  }
  if (original != null) return; // Shown already.
  original = link.getAttribute("href") ?? "/favicon.ico";
  const href = original;
  try {
    badged ??= await drawBadge(href);
  } catch {
    return; // No canvas, the icon didn't load: no badge, the other signals remain.
  }
  // Removed while drawing?
  if (original === href) link.href = badged;
}

async function drawBadge(href: string): Promise<string> {
  const image = new Image();
  image.src = href;
  await image.decode();
  const canvas = document.createElement("canvas");
  canvas.width = SIZE;
  canvas.height = SIZE;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("no canvas");
  ctx.drawImage(image, 0, 0, SIZE, SIZE);
  ctx.beginPath();
  ctx.arc(SIZE - 8, 8, 7, 0, 2 * Math.PI);
  // The colour token of the theme (a canvas understands oklch), red where it can't be read.
  const destructive = getComputedStyle(document.documentElement)
    .getPropertyValue("--destructive")
    .trim();
  ctx.fillStyle = destructive || "red";
  ctx.fill();
  ctx.lineWidth = 2;
  ctx.strokeStyle = "white";
  ctx.stroke();
  return canvas.toDataURL("image/png");
}
