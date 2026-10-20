"use client";

import { useMemo, useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Tick02Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { SearchInput } from "@/components/shared/list/search-input";
import type { MenuItem } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { CustomizerPanel } from "./customizer-panel";
import { containsItem, itemId } from "./menu-tree";
import { TemplateChip } from "./template-chip";

/** The template: the main menu by its categories, searchable, each category collapsible. */
export function TemplatePanel({
  categories,
  menu,
  onAdd,
}: {
  categories: MenuItem[];
  /** The user's menu, to tick the entries already in it. */
  menu: MenuItem[];
  onAdd: (item: MenuItem) => void;
}) {
  const t = useTranslations();
  const [search, setSearch] = useState("");
  const [collapsed, setCollapsed] = useState<ReadonlySet<string>>(new Set());

  const term = search.trim().toLowerCase();
  const shown = useMemo(
    () =>
      categories
        .map((category) => ({
          category,
          entries: (category.subMenu ?? []).filter(
            (entry) => !term || entry.title.toLowerCase().includes(term)
          ),
        }))
        .filter(({ entries }) => entries.length > 0),
    [categories, term]
  );

  const toggle = (id: string, open: boolean) =>
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (open) next.delete(id);
      else next.add(id);
      return next;
    });

  return (
    <CustomizerPanel
      title={t("menu.customizer.templateTitle")}
      label={t("menu.customizer.templateLabel")}
    >
      <div className="relative">
        <SearchInput value={search} onChange={setSearch} />
      </div>
      <p className="text-xs text-muted-foreground">
        {t("menu.customizer.searchHint")}
      </p>
      <div className="flex flex-col gap-1" data-testid="menu-template">
        {shown.map(({ category, entries }) => {
          const id = itemId(category);
          // A search shows every match, whatever was collapsed before.
          const open = !!term || !collapsed.has(id);
          const added = entries.filter((e) =>
            containsItem(menu, itemId(e))
          ).length;
          return (
            <Collapsible
              key={id}
              open={open}
              onOpenChange={(o) => toggle(id, o)}
            >
              <CollapsibleTrigger className="flex w-full items-center gap-2 rounded-md px-1 py-1.5 text-left text-sm font-medium hover:bg-muted">
                <HugeiconsIcon
                  icon={ArrowDown01Icon}
                  size={14}
                  className={cn("transition-transform", !open && "-rotate-90")}
                />
                <span className="truncate">{category.title}</span>
                <Badge
                  variant={added > 0 ? "default" : "secondary"}
                  className="ml-auto"
                >
                  {added > 0 && <HugeiconsIcon icon={Tick02Icon} />}
                  {added > 0 ? `${added}/${entries.length}` : entries.length}
                </Badge>
              </CollapsibleTrigger>
              <CollapsibleContent className="flex flex-wrap gap-1.5 px-1 pb-2 pt-1">
                {entries.map((entry) => (
                  <TemplateChip
                    key={itemId(entry)}
                    item={entry}
                    added={containsItem(menu, itemId(entry))}
                    onAdd={() => onAdd(entry)}
                  />
                ))}
              </CollapsibleContent>
            </Collapsible>
          );
        })}
        {shown.length === 0 && (
          <p className="py-6 text-center text-sm text-muted-foreground">
            {t("menu.customizer.noResults")}
          </p>
        )}
      </div>
    </CustomizerPanel>
  );
}
