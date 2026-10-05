"use client";

import { Fragment, useMemo, useRef, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SearchInput } from "@/components/shared/list/search-input";
import { useTextHighlights } from "@/hooks/use-text-highlights";
import changelogJson from "@/lib/generated/changelog.json";
import { ChangelogNewsCard } from "./changelog-news";
import { MatchStepper } from "./match-stepper";
import { ReleaseEntry } from "./release-entry";
import { filterRelease, newsMatches, searchTerms } from "./search";
import type { Changelog } from "./types";

const WEBSITE_CHANGELOG_URL = "https://www.projectforge.org/changelog-posts/";

/**
 * Generated from changelog/changelog.json by GenerateChangelogMain (bin/pfDev.sh gen), the same source as
 * the website's changelog — never edited by hand. Imported at build time, the static export needs no REST call.
 */
const changelog = changelogJson as Changelog;

/**
 * The changelog page (`/next/changelog`), reached from the version in the status bar and the user menu:
 * every release and snapshot milestone, newest first and open, as collapsible sections, each news of a
 * version right above its newest release. The search field searches all of it, folded or not, and shows
 * only what matches, unfolded, every occurrence highlighted and stepped through. The texts are English only, like the website's changelog.
 */
export function ChangelogPage() {
  const t = useTranslations();
  const locale = useLocale();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState<Set<string>>(
    () => new Set(changelog.releases.slice(0, 1).map((release) => release.id))
  );

  const terms = useMemo(() => searchTerms(query), [query]);
  const searching = terms.length > 0;
  // Per release: itself reduced to what matches, and the news shown above it — a news matching on its
  // own shows even if nothing in its release matches.
  const rows = useMemo(
    () =>
      changelog.releases
        .map((original) => ({
          id: original.id,
          release: filterRelease(original, terms),
          news: changelog.news.find(
            (news) => news.releaseId === original.id && newsMatches(news, terms)
          ),
        }))
        .filter((row) => row.release !== null || row.news !== undefined),
    [terms]
  );

  const results = useRef<HTMLDivElement>(null);
  const matches = useTextHighlights(results, terms);

  const search = (value: string) => {
    setQuery(value);
    // The results are shown unfolded; they can be folded one by one from there.
    if (searchTerms(value).length > 0) {
      setOpen(new Set(changelog.releases.map((release) => release.id)));
    }
  };

  const toggle = (id: string, isOpen: boolean) =>
    setOpen((current) => {
      const next = new Set(current);
      if (isOpen) next.add(id);
      else next.delete(id);
      return next;
    });

  return (
    <PageShell>
      <PageTitleRow title={t("changelog.title")}>
        <div className="relative w-64">
          <SearchInput
            value={query}
            onChange={search}
            autoFocus
            onEnter={(backwards) =>
              backwards ? matches.previous() : matches.next()
            }
          />
        </div>
        {searching && (
          <MatchStepper
            count={matches.count}
            current={matches.current}
            onPrevious={matches.previous}
            onNext={matches.next}
          />
        )}
        <Button
          variant="outline"
          size="sm"
          onClick={() =>
            setOpen(new Set(changelog.releases.map((release) => release.id)))
          }
        >
          {t("changelog.expandAll")}
        </Button>
        <Button variant="outline" size="sm" onClick={() => setOpen(new Set())}>
          {t("changelog.collapseAll")}
        </Button>
        <a
          href={WEBSITE_CHANGELOG_URL}
          target="_blank"
          rel="noreferrer"
          className="text-xs text-primary hover:underline"
        >
          {t("changelog.website")}
        </a>
      </PageTitleRow>
      <div
        ref={results}
        className="flex max-w-4xl flex-col gap-2 px-4 pb-6 pt-2 text-sm"
      >
        {locale !== "en" && (
          <p className="text-xs text-muted-foreground">
            {t("changelog.englishOnly")}
          </p>
        )}
        {rows.map(({ id, release, news }) => (
          <Fragment key={id}>
            {news && (
              <div className="mt-4 first:mt-0">
                <ChangelogNewsCard news={news} />
              </div>
            )}
            {release && (
              <ReleaseEntry
                release={release}
                open={open.has(id)}
                onOpenChange={(isOpen) => toggle(id, isOpen)}
                searching={searching}
              />
            )}
          </Fragment>
        ))}
        {rows.length === 0 && (
          <p className="text-muted-foreground">{t("changelog.noMatches")}</p>
        )}
      </div>
    </PageShell>
  );
}
