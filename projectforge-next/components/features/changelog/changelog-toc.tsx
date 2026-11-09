"use client";

import { type RefObject, useEffect, useMemo, useRef } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { releaseAnchor } from "./release-entry";
import { useActiveAnchor } from "./use-active-anchor";
import type { ChangelogGroup, ChangelogRelease } from "./types";

/** The anchor of a major, the slug of its article on the website (`site/_changelogs/changelog-major-<n>.adoc`). */
export const majorAnchor = (major: string) => `changelog-major-${major}`;

/** The anchor of the folded snapshots of a major. */
export const snapshotsAnchor = (major: string) => `snapshots-${major}`;

interface TocEntry {
  id: string;
  label: string;
  /** The major the entry belongs to, undefined for the major itself and the entries not released yet. */
  major?: string;
}

interface TocMajor {
  entry: TocEntry;
  children: TocEntry[];
}

/** The entries of the navigation, in the order of the page: the majors with their updates, head and snapshots. */
function tocMajors(
  groups: ChangelogGroup[],
  snapshotsLabel: string
): TocMajor[] {
  return groups.map((group) => {
    const release = (r: ChangelogRelease): TocEntry => ({
      id: releaseAnchor(r),
      label: `ProjectForge ${r.version}`,
      major: group.major,
    });
    const children = group.updates.map(release);
    if (group.head) children.push(release(group.head));
    if (group.snapshots.length > 0) {
      children.push({
        id: snapshotsAnchor(group.major),
        label: `${snapshotsLabel} (${group.snapshots.length})`,
        major: group.major,
      });
    }
    return {
      entry: {
        id: majorAnchor(group.major),
        label: `ProjectForge ${group.major}`,
      },
      children,
    };
  });
}

/**
 * The table of contents of the changelog page, as on the website: the entries not released yet, the majors and,
 * below the major in view, its releases and snapshots. It lists what the page shows, so a search narrows it too.
 * The entry in view is highlighted and kept visible in the list. [container]: an element inside the scroll column
 * of the page; [onNavigate]: scrolls to an entry (and unfolds the snapshots).
 */
export function ChangelogToc({
  unreleased,
  groups,
  container,
  onNavigate,
  className,
}: {
  unreleased: ChangelogRelease | null;
  groups: ChangelogGroup[];
  container: RefObject<HTMLElement | null>;
  onNavigate: (id: string) => void;
  className?: string;
}) {
  const t = useTranslations();
  const snapshotsLabel = t("changelog.snapshots");
  const majors = useMemo(
    () => tocMajors(groups, snapshotsLabel),
    [groups, snapshotsLabel]
  );
  const ids = useMemo(() => {
    const all = unreleased ? [releaseAnchor(unreleased)] : [];
    majors.forEach((major) =>
      all.push(major.entry.id, ...major.children.map((child) => child.id))
    );
    return all;
  }, [unreleased, majors]);
  const active = useActiveAnchor(container, ids);
  const activeMajor =
    majors.find(
      (major) =>
        major.entry.id === active ||
        major.children.some((child) => child.id === active)
    ) ?? majors[0];

  // Keeps the highlighted link visible in the list, without scrolling the page as scrollIntoView would.
  const list = useRef<HTMLElement>(null);
  useEffect(() => {
    const box = list.current;
    const link = box?.querySelector<HTMLElement>('[aria-current="location"]');
    if (!box || !link || box.scrollHeight <= box.clientHeight) return;
    const linkRect = link.getBoundingClientRect();
    const boxRect = box.getBoundingClientRect();
    if (linkRect.top < boxRect.top + 40) {
      box.scrollTop += linkRect.top - boxRect.top - 40;
    } else if (linkRect.bottom > boxRect.bottom - 40) {
      box.scrollTop += linkRect.bottom - boxRect.bottom + 40;
    }
  }, [active]);

  const link = (entry: TocEntry, sub = false) => (
    <a
      href={`#${entry.id}`}
      aria-current={entry.id === active ? "location" : undefined}
      onClick={(event) => {
        if (
          event.button !== 0 ||
          event.metaKey ||
          event.ctrlKey ||
          event.shiftKey ||
          event.altKey
        )
          return;
        event.preventDefault();
        onNavigate(entry.id);
      }}
      className={cn(
        "block rounded-md px-2 py-1 no-underline",
        sub ? "text-xs" : "text-[13px] font-medium",
        entry.id === active
          ? "bg-accent text-primary"
          : "text-muted-foreground hover:bg-muted hover:text-foreground"
      )}
    >
      {entry.label}
    </a>
  );

  return (
    <nav
      ref={list}
      aria-label={t("changelog.toc.title")}
      className={cn("overflow-y-auto", className)}
    >
      <ul className="flex flex-col gap-0.5">
        {unreleased && (
          <li>
            {link({ id: releaseAnchor(unreleased), label: unreleased.title })}
          </li>
        )}
        {majors.map((major) => (
          <li key={major.entry.id}>
            {link(major.entry)}
            {major === activeMajor && major.children.length > 0 && (
              <ul className="my-0.5 ml-2 flex flex-col gap-0.5 border-l pl-2">
                {major.children.map((child) => (
                  <li key={child.id}>{link(child, true)}</li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </nav>
  );
}
