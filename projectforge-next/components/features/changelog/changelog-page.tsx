"use client";

import { type ReactNode, useMemo, useRef, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SearchInput } from "@/components/shared/list/search-input";
import { useTextHighlights } from "@/hooks/use-text-highlights";
import changelogEn from "@/lib/generated/changelog.json";
import changelogDe from "@/lib/generated/changelog.de.json";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible";
import { CollapsibleTrigger } from "@/components/shared/copyable-collapsible-trigger";
import { MatchStepper } from "./match-stepper";
import { ReleaseEntry, type ReleaseLevel } from "./release-entry";
import { filterRelease, searchTerms } from "./search";
import type { Changelog, ChangelogGroup, ChangelogRelease } from "./types";

const WEBSITE_CHANGELOG_URL = "https://www.projectforge.org/changelog-posts/";

/**
 * Generated from changelog/changelog.json (and its German translation changelog.de.json) by
 * GenerateChangelogMain (bin/pfDev.sh gen), the same source as the website's changelog — never edited by
 * hand. Imported at build time, the static export needs no REST call.
 */
const CHANGELOGS: Record<string, Changelog> = {
  en: changelogEn as Changelog,
  de: changelogDe as Changelog,
};

/** The key of an unfolded part of the page: a level of a release, or the snapshots of a major. */
const levelKey = (id: string, level: ReleaseLevel) => `${id}:${level}`;
const snapshotsKey = (major: string) => `snapshots:${major}`;

/** Every key of the page, for "expand all" and a search showing everything it found. */
function allKeys(changelog: Changelog): Set<string> {
  const keys = new Set<string>();
  const add = (release: ChangelogRelease) => {
    keys.add(levelKey(release.id, "overview"));
    keys.add(levelKey(release.id, "all"));
  };
  if (changelog.unreleased) add(changelog.unreleased);
  changelog.groups.forEach((group) => {
    keys.add(snapshotsKey(group.major));
    [group.head, ...group.updates, ...group.snapshots].forEach(
      (release) => release && add(release)
    );
  });
  return keys;
}

/** The group reduced to its releases matching [terms], null if none does. */
function filterGroup(
  group: ChangelogGroup,
  terms: string[]
): ChangelogGroup | null {
  const filter = (releases: ChangelogRelease[]) =>
    releases
      .map((release) => filterRelease(release, terms))
      .filter((release): release is ChangelogRelease => release !== null);
  const head = group.head ? filterRelease(group.head, terms) : null;
  const updates = filter(group.updates);
  const snapshots = filter(group.snapshots);
  if (!head && updates.length === 0 && snapshots.length === 0) return null;
  return { major: group.major, head: head ?? undefined, updates, snapshots };
}

/**
 * The changelog page (`/next/changelog`), reached from the version in the status bar and the user menu:
 * the releases grouped by major version, newest first — the release opening a major, its updates and,
 * folded, the snapshots leading up to it. Each release shows its summary, its overview and all its changes
 * are unfolded with one click each. The entries not released yet are shown on top. The search field
 * searches all of it, folded or not, and shows only what matches, unfolded, every occurrence highlighted
 * and stepped through. The texts are German for the German locale, English otherwise (the website's
 * changelog is English only).
 */
export function ChangelogPage() {
  const t = useTranslations();
  const changelog = CHANGELOGS[useLocale()] ?? CHANGELOGS.en;
  const [query, setQuery] = useState("");
  // Not released yet, the entries have no summary: they are shown unfolded.
  const [open, setOpen] = useState<Set<string>>(
    () => new Set([levelKey("unreleased", "all")])
  );

  const terms = useMemo(() => searchTerms(query), [query]);
  const searching = terms.length > 0;
  const unreleased = useMemo(
    () =>
      changelog.unreleased ? filterRelease(changelog.unreleased, terms) : null,
    [changelog, terms]
  );
  const groups = useMemo(
    () =>
      changelog.groups
        .map((group) => filterGroup(group, terms))
        .filter((group): group is ChangelogGroup => group !== null),
    [changelog, terms]
  );

  const results = useRef<HTMLDivElement>(null);
  const matches = useTextHighlights(results, terms);

  const search = (value: string) => {
    setQuery(value);
    // The results are shown unfolded; they can be folded one by one from there.
    if (searchTerms(value).length > 0) setOpen(allKeys(changelog));
  };

  const toggle = (key: string, isOpen: boolean) =>
    setOpen((current) => {
      const next = new Set(current);
      if (isOpen) next.add(key);
      else next.delete(key);
      return next;
    });

  const entry = (release: ChangelogRelease, head = false) => (
    <ReleaseEntry
      key={release.id}
      release={release}
      head={head}
      searching={searching}
      isOpen={(level) => open.has(levelKey(release.id, level))}
      onToggle={(level, isOpen) => toggle(levelKey(release.id, level), isOpen)}
    />
  );

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
          onClick={() => setOpen(allKeys(changelog))}
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
        className="flex max-w-4xl flex-col gap-6 px-4 pb-6 pt-2 text-[13px] font-normal leading-relaxed"
      >
        {unreleased && entry(unreleased)}
        {groups.map((group) => (
          <section key={group.major} className="flex flex-col gap-2">
            <h2 className="text-base font-semibold">
              ProjectForge {group.major}
            </h2>
            {group.head && entry(group.head, true)}
            {group.updates.map((release) => entry(release))}
            {group.snapshots.length > 0 && (
              <Snapshots
                count={group.snapshots.length}
                open={open.has(snapshotsKey(group.major))}
                onOpenChange={(isOpen) =>
                  toggle(snapshotsKey(group.major), isOpen)
                }
              >
                {group.snapshots.map((release) => entry(release))}
              </Snapshots>
            )}
          </section>
        ))}
        {!unreleased && groups.length === 0 && (
          <p className="text-muted-foreground">{t("changelog.noMatches")}</p>
        )}
      </div>
    </PageShell>
  );
}

/** The snapshots of develop leading up to a major, folded at first. */
function Snapshots({
  count,
  open,
  onOpenChange,
  children,
}: {
  count: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: ReactNode;
}) {
  const t = useTranslations();
  return (
    <Collapsible open={open} onOpenChange={onOpenChange}>
      <CollapsibleTrigger className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:underline">
        <HugeiconsIcon
          icon={open ? ArrowDown01Icon : ArrowRight01Icon}
          size={12}
        />
        {t("changelog.snapshots")} ({count})
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="mt-2 flex flex-col gap-2 border-l-2 pl-3">
          {children}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}
