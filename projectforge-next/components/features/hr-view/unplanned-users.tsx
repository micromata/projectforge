"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { MenuLink } from "@/components/shared/menu-link";
import { SearchInput } from "@/components/shared/list/search-input";
import { planningHref } from "./planning-links";
import type { HrView, HrViewUser } from "./types";

/** Whether every word of the search occurs in the name, case-insensitive; an empty search matches all. */
function matches(user: HrViewUser, search: string): boolean {
  const name = (user.displayName ?? String(user.id)).toLowerCase();
  return search
    .toLowerCase()
    .split(/\s+/)
    .every((word) => name.includes(word));
}

/**
 * The employees with HR planning but no planned week in the period ("Nicht geplant"), each leading to a new
 * planning of the period's first week where the account may write plannings. The list is long in a real
 * company, so a search box narrows it — on the names already loaded, no request.
 */
export function UnplannedUsers({ view }: { view: HrView }) {
  const t = useTranslations();
  const [search, setSearch] = useState("");
  const users = view.unplannedUsers.filter((user) =>
    matches(user, search.trim())
  );

  return (
    <div className="flex flex-col gap-2 text-sm">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <span className="font-medium">{t("hr.planning.notPlanned")}:</span>
        <div className="relative w-64 max-w-full">
          <SearchInput value={search} onChange={setSearch} />
        </div>
      </div>
      <p className="flex flex-wrap gap-x-3 gap-y-1">
        {users.map((user) => {
          const name = user.displayName ?? String(user.id);
          const url = planningHref(view, user.id);
          return url ? (
            <MenuLink
              key={user.id}
              url={url}
              className="text-primary hover:underline"
            >
              {name}
            </MenuLink>
          ) : (
            <span key={user.id}>{name}</span>
          );
        })}
      </p>
    </div>
  );
}
