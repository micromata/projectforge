"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import { ChangelogText } from "./changelog-text";
import { ReleaseSection } from "./release-section";
import type { ChangelogRelease } from "./types";

const REPO_URL = "https://github.com/micromata/projectforge";

/** The levels of a release unfolded on request: its overview (level 2) and all its changes (level 3). */
export type ReleaseLevel = "overview" | "all";

/**
 * One release or snapshot milestone: title, date and source, its summary (level 1) always shown, and two
 * toggles side by side, each unfolding one more level with a single click — the overview and all changes
 * (intro and sections). [head]: the release opening a major, set apart by its accent border.
 */
export function ReleaseEntry({
  release,
  isOpen,
  onToggle,
  searching = false,
  head = false,
}: {
  release: ChangelogRelease;
  isOpen: (level: ReleaseLevel) => boolean;
  onToggle: (level: ReleaseLevel, open: boolean) => void;
  /** The release shows search results, see [ReleaseSection]. */
  searching?: boolean;
  head?: boolean;
}) {
  const t = useTranslations();
  const hasOverview = (release.overview?.length ?? 0) > 0;
  const hasAll = release.sections.length > 0 || (release.intro?.length ?? 0) > 0;
  const overviewOpen = hasOverview && isOpen("overview");
  const allOpen = hasAll && isOpen("all");
  return (
    <article
      className={cn(
        "flex flex-col gap-2 rounded-md border bg-card px-3 py-2 shadow-sm",
        head && "border-l-4 border-l-primary"
      )}
    >
      <header className="flex items-baseline gap-2">
        <h3 className="font-medium">{release.title}</h3>
        <span className="ml-auto shrink-0 text-xs text-muted-foreground">
          {release.date}
        </span>
      </header>
      <ReleaseSource release={release} />
      {release.summary && release.summary.length > 0 && (
        <ul className="list-disc space-y-0.5 pl-5">
          {release.summary.map((text) => (
            <li key={text}>
              <ChangelogText text={text} />
            </li>
          ))}
        </ul>
      )}
      {(hasOverview || hasAll) && (
        <div className="flex gap-4">
          {hasOverview && (
            <LevelToggle
              label={t("changelog.overview")}
              open={overviewOpen}
              onClick={() => onToggle("overview", !overviewOpen)}
            />
          )}
          {hasAll && (
            <LevelToggle
              label={t("changelog.allChanges")}
              open={allOpen}
              onClick={() => onToggle("all", !allOpen)}
            />
          )}
        </div>
      )}
      {overviewOpen && (
        <ul className="list-disc space-y-0.5 border-t pl-5 pt-2">
          {release.overview!.map((text) => (
            <li key={text}>
              <ChangelogText text={text} />
            </li>
          ))}
        </ul>
      )}
      {allOpen && (
        <div className="flex flex-col gap-3 border-t pt-2">
          {release.intro?.map((intro) => (
            <ChangelogText key={intro} text={intro} />
          ))}
          {release.sections.map((section, index) => (
            <ReleaseSection
              key={`${section.type}-${index}`}
              section={section}
              searching={searching}
            />
          ))}
        </div>
      )}
    </article>
  );
}

function LevelToggle({
  label,
  open,
  onClick,
}: {
  label: string;
  open: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      aria-expanded={open}
      onClick={onClick}
      className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
    >
      <HugeiconsIcon icon={open ? ArrowDown01Icon : ArrowRight01Icon} size={12} />
      {label}
    </button>
  );
}

/** Where the release lives in git: its tag, or for a snapshot the end commit and the compare view. */
function ReleaseSource({ release }: { release: ChangelogRelease }) {
  const t = useTranslations();
  const linkClass = "font-mono hover:underline";
  if (release.tag) {
    return (
      <p className="text-xs text-muted-foreground">
        {t("changelog.tag")}{" "}
        <a
          href={`${REPO_URL}/tree/${release.tag}`}
          target="_blank"
          rel="noreferrer"
          className={linkClass}
        >
          {release.tag}
        </a>
      </p>
    );
  }
  if (!release.fromCommit || !release.toCommit) return null;
  return (
    <p className="text-xs text-muted-foreground">
      {t("changelog.snapshot")}{" "}
      <a
        href={`${REPO_URL}/commit/${release.toCommit}`}
        target="_blank"
        rel="noreferrer"
        className={linkClass}
      >
        develop@{release.toCommit}
      </a>
      {" · "}
      <a
        href={`${REPO_URL}/compare/${release.fromCommit}..${release.toCommit}`}
        target="_blank"
        rel="noreferrer"
        className="hover:underline"
      >
        {t("changelog.compare")}
      </a>
    </p>
  );
}
