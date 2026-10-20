"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { SearchInput } from "@/components/shared/list/search-input";
import { useMenuLayout } from "@/components/shared/main-menu-browse";
import { balanceMenuColumns } from "@/lib/menu-columns";
import type { MenuItem } from "@/lib/rs/types";
import { containsItem, itemId } from "./menu-tree";
import { TemplateChip } from "./template-chip";

/** The template: the main menu in balanced columns of categories, as its dropdown shows it, searchable. */
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
  const { columns } = useMenuLayout();

  const term = search.trim().toLowerCase();
  const balanced = useMemo(
    () =>
      balanceMenuColumns(
        categories.map((category) => ({
          ...category,
          subMenu: (category.subMenu ?? []).filter(
            (entry) => !term || entry.title.toLowerCase().includes(term)
          ),
        })),
        columns
      ),
    [categories, term, columns]
  );

  return (
    <section className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <h2 className="text-sm font-semibold">
          {t("menu.customizer.templateTitle")}
        </h2>
        <p className="text-xs text-muted-foreground">
          {t("menu.customizer.searchHint")}
        </p>
        <div className="relative w-full sm:ml-auto sm:w-64">
          <SearchInput value={search} onChange={setSearch} />
        </div>
      </div>
      <div className="flex items-start gap-6" data-testid="menu-template">
        {balanced.map((column, index) => (
          // basis-0: the columns share the width evenly, as in the main menu (MainMenuBrowse).
          <div
            key={index}
            className="flex min-w-0 flex-1 basis-0 flex-col gap-4"
          >
            {column.map((category) => (
              <TemplateCategory
                key={itemId(category)}
                category={category}
                menu={menu}
                onAdd={onAdd}
              />
            ))}
          </div>
        ))}
        {balanced.length === 0 && (
          <p className="w-full py-6 text-center text-sm text-muted-foreground">
            {t("menu.customizer.noResults")}
          </p>
        )}
      </div>
    </section>
  );
}

function TemplateCategory({
  category,
  menu,
  onAdd,
}: {
  category: MenuItem;
  menu: MenuItem[];
  onAdd: (item: MenuItem) => void;
}) {
  const entries = category.subMenu ?? [];
  return (
    <div className="flex flex-col gap-0.5">
      {/* The brand-teal label of the main menu's columns (MenuColumnHeading). */}
      <span className="truncate px-2 py-1 text-xs font-semibold tracking-wide text-brand-teal uppercase">
        {category.title}
      </span>
      {entries.map((entry) => (
        <TemplateChip
          key={itemId(entry)}
          item={entry}
          added={containsItem(menu, itemId(entry))}
          onAdd={() => onAdd(entry)}
        />
      ))}
    </div>
  );
}
