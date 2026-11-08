import { find } from "linkifyjs";

/** Where a url is never turned into a link: in a link already, in code and in a mention. */
const SKIPPED = "a, code, pre, [data-type='mention']";

/**
 * The urls in [text] to link, as linkifyjs (the detection of the editor's autolink) finds them, but only
 * the ones with `http(s)://` or `www.`: a bare name like `pfDev.sh` or `example.com` stays text. A `www.`
 * url links to https.
 */
export function findUrls(
  text: string
): { start: number; end: number; href: string }[] {
  return find(text, "url")
    .filter((it) => /^(https?:\/\/|www\.)/i.test(it.value))
    .map((it) => ({
      start: it.start,
      end: it.end,
      href: /^www\./i.test(it.value) ? `https://${it.value}` : it.href,
    }));
}

/**
 * The (sanitized) HTML of a rich text or a chat message with its urls as links, opened in a new tab
 * like every link of a rich text: a url pasted or typed as its last word (the editor links only after a
 * space) or written before the editor had links is clickable too. Only the display is changed, the
 * stored text stays as it is. Unchanged where there is no DOM (prerendering).
 */
export function linkifyHtml(html: string): string {
  if (typeof document === "undefined" || !/https?:\/\/|www\./i.test(html)) {
    return html;
  }
  // The content of a template is inert: parsing it runs no scripts and loads nothing.
  const template = document.createElement("template");
  template.innerHTML = html;
  const walker = document.createTreeWalker(
    template.content,
    NodeFilter.SHOW_TEXT
  );
  const texts: Text[] = [];
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    if (!node.parentElement?.closest(SKIPPED)) texts.push(node as Text);
  }
  let changed = false;
  texts.forEach((node) => {
    const text = node.data;
    const urls = findUrls(text);
    if (urls.length === 0) return;
    const parts = document.createDocumentFragment();
    let last = 0;
    urls.forEach(({ start, end, href }) => {
      parts.append(text.slice(last, start));
      const link = document.createElement("a");
      link.href = href;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      link.textContent = text.slice(start, end);
      parts.append(link);
      last = end;
    });
    parts.append(text.slice(last));
    node.replaceWith(parts);
    changed = true;
  });
  return changed ? template.innerHTML : html;
}
