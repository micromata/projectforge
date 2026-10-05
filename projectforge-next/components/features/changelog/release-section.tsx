"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Collapsible, CollapsibleContent } from "@/components/ui/collapsible";
import { CollapsibleTrigger } from "@/components/shared/copyable-collapsible-trigger";
import { ChangelogText } from "./changelog-text";
import type { ChangelogSection } from "./types";

/**
 * The i18n keys of the labels of the section types (`site/_data/tags.yml`), spelled out for the i18n scan;
 * the colors are the `--changelog-<type>` tokens.
 */
const TYPE_LABELS: Record<string, string> = {
  added: "changelog.type.added",
  admin: "changelog.type.admin",
  changed: "changelog.type.changed",
  deprecated: "changelog.type.deprecated",
  improved: "changelog.type.improved",
  removed: "changelog.type.removed",
  fixed: "changelog.type.fixed",
  privacy: "changelog.type.privacy",
  security: "changelog.type.security",
  technology: "changelog.type.technology",
  docker: "changelog.type.docker",
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
  const t = useTranslations();
  const label = TYPE_LABELS[section.type];
  return (
    <div className="flex flex-col gap-1">
      <span
        className="inline-flex h-5 w-fit items-center rounded-sm px-2 text-[0.625rem] font-semibold uppercase tracking-wide text-white"
        style={{ background: `var(--changelog-${section.type})` }}
      >
        {label ? t(label) : section.type}
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
        <CollapsibleTrigger className="-ml-5 flex items-baseline gap-1 text-left font-medium hover:underline">
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
