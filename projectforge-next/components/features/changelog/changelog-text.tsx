"use client";

import { useTranslations } from "next-intl";
import { MarkdownText } from "@/components/shared/markdown-text";

/** The marker of a change of the mobile app, at the start of a text (see GenerateChangelogMain). */
const APP_MARKER = "{app} ";

/**
 * A text of the changelog: the markdown subset of changelog/changelog.json with its extensions
 * `{red}…{/red}` turned into a span and a leading `{app} ` into the badge "App". Safe as raw HTML: the
 * texts are authored in the repository and the generator rejects any HTML in them.
 */
export function ChangelogText({
  text,
  className,
}: {
  text: string;
  className?: string;
}) {
  const t = useTranslations();
  const app = text.startsWith(APP_MARKER);
  const badge = `<span class="mr-1 inline-flex bg-[var(--changelog-app)] h-4 items-center rounded-sm px-1.5 align-[1px] text-[0.625rem] font-semibold uppercase tracking-wide text-white">${t("changelog.app")}</span>`;
  const html = (app ? badge + text.slice(APP_MARKER.length) : text).replace(
    /\{red\}(.*?)\{\/red\}/g,
    '<span class="text-destructive">$1</span>'
  );
  return <MarkdownText text={html} allowHtml className={className} />;
}
