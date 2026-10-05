"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible";
import { CollapsibleTrigger } from "@/components/shared/copyable-collapsible-trigger";
import { ChangelogText } from "./changelog-text";
import { ReleaseSection } from "./release-section";
import type { ChangelogRelease } from "./types";

const REPO_URL = "https://github.com/micromata/projectforge";

/** One release or snapshot milestone, collapsible: title and date as the trigger, intro and sections below. */
export function ReleaseEntry({
  release,
  open,
  onOpenChange,
  searching = false,
}: {
  release: ChangelogRelease;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The release shows search results, see [ReleaseSection]. */
  searching?: boolean;
}) {
  return (
    <Collapsible
      open={open}
      onOpenChange={onOpenChange}
      className="overflow-hidden rounded-md border bg-card shadow-sm"
    >
      <CollapsibleTrigger className="flex w-full items-baseline gap-2 px-3 py-2 text-left hover:bg-muted/40">
        <HugeiconsIcon
          icon={open ? ArrowDown01Icon : ArrowRight01Icon}
          size={14}
          className="self-center text-muted-foreground"
        />
        <span className="font-semibold">{release.title}</span>
        <span className="ml-auto shrink-0 text-xs text-muted-foreground">
          {release.date}
        </span>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="flex flex-col gap-3 border-t px-3 py-3">
          {release.intro?.map((intro) => (
            <ChangelogText key={intro} text={intro} />
          ))}
          <ReleaseSource release={release} />
          {release.sections.map((section, index) => (
            <ReleaseSection
              key={`${section.type}-${index}`}
              section={section}
              searching={searching}
            />
          ))}
        </div>
      </CollapsibleContent>
    </Collapsible>
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
