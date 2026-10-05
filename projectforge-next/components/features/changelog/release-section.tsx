"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible";
import { CollapsibleTrigger } from "@/components/shared/copyable-collapsible-trigger";
import { ChangelogText } from "./changelog-text";
import type { ChangelogSection } from "./types";

/**
 * Labels of the section types, as the website shows them (`site/_data/tags.yml`). English like the
 * changelog texts they head, so not translated; the colors are the `--changelog-<type>` tokens.
 */
const TYPE_LABELS: Record<string, string> = {
  added: "Added",
  admin: "Admin",
  changed: "Changed",
  deprecated: "Deprecated",
  improved: "Improved",
  removed: "Removed",
  fixed: "Fixed",
  privacy: "Privacy",
  security: "Security",
  technology: "Technology",
  docker: "Docker",
};

/**
 * One typed section of a release: its colored label and the items, groups collapsible on their own.
 * [searching]: the items are search results, so a group can't be folded away.
 */
export function ReleaseSection({
  section,
  searching = false,
}: {
  section: ChangelogSection;
  searching?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1">
      <span
        className="inline-flex h-5 w-fit items-center rounded-sm px-2 text-[0.625rem] font-semibold uppercase tracking-wide text-white"
        style={{ background: `var(--changelog-${section.type})` }}
      >
        {TYPE_LABELS[section.type] ?? section.type}
      </span>
      <ul className="list-disc space-y-0.5 pl-5">
        {section.items.map((item) =>
          typeof item === "string" ? (
            <li key={item}>
              <ChangelogText text={item} />
            </li>
          ) : (
            <ItemGroup
              key={item.title}
              title={item.title}
              items={item.items}
              searching={searching}
            />
          )
        )}
      </ul>
    </div>
  );
}

/** A group of items under a title, open at first: it is part of the release text, folding is optional. */
function ItemGroup({
  title,
  items,
  searching,
}: {
  title: string;
  items: string[];
  searching: boolean;
}) {
  const [expanded, setOpen] = useState(true);
  const open = searching || expanded;
  return (
    <li className="list-none">
      <Collapsible open={open} onOpenChange={setOpen}>
        <CollapsibleTrigger className="-ml-5 flex items-baseline gap-1 text-left font-semibold hover:underline">
          <HugeiconsIcon
            icon={open ? ArrowDown01Icon : ArrowRight01Icon}
            size={12}
            className="self-center text-muted-foreground"
          />
          {title}
        </CollapsibleTrigger>
        <CollapsibleContent>
          <ul className="list-[circle] space-y-0.5 pl-5">
            {items.map((child) => (
              <li key={child}>
                <ChangelogText text={child} />
              </li>
            ))}
          </ul>
        </CollapsibleContent>
      </Collapsible>
    </li>
  );
}
