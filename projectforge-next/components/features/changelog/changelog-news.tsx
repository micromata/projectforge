"use client";

import { useTranslations } from "next-intl";
import { ChangelogText } from "./changelog-text";
import type { ChangelogNews } from "./types";

/**
 * A news of the changelog: a few highlights of a version, shown above the newest release of that version
 * and set apart from the releases by its accent border.
 */
export function ChangelogNewsCard({ news }: { news: ChangelogNews }) {
  const t = useTranslations();
  return (
    <article className="rounded-md border border-l-4 border-l-primary bg-card p-3 shadow-sm">
      <p className="text-[0.625rem] font-semibold uppercase tracking-wide text-primary">
        {t("changelog.news")}
      </p>
      <h3 className="font-semibold">{news.title}</h3>
      <p className="mb-2 text-xs text-muted-foreground">
        {news.version} · {news.date}
      </p>
      <ChangelogText text={news.text} />
      {news.highlights && news.highlights.length > 0 && (
        <ul className="mt-2 list-disc space-y-0.5 pl-5">
          {news.highlights.map((highlight) => (
            <li key={highlight}>
              <ChangelogText text={highlight} />
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}
