"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import {
  EntityMultiAutocomplete,
  type EntityMultiAutocompleteProps,
} from "@/components/shared/entity-multi-autocomplete";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import type { GroupRef, UserRef } from "@/lib/rs/calendar-types";

interface CalendarVacationSelectsProps {
  groups: GroupRef[];
  users: UserRef[];
  onGroupsChange: (ids: number[]) => void;
  onUsersChange: (ids: number[]) => void;
}

/** A minimal ref (`GroupRef`/`UserRef`) as the multi-select consumes it — `displayName` falls back to name/username. */
function toEntityRef(ref: GroupRef | UserRef): EntityRef {
  const name =
    ref.displayName ??
    ("name" in ref ? ref.name : undefined) ??
    ("username" in ref ? ref.username : undefined) ??
    "";
  return { id: ref.id ?? 0, displayName: name };
}

/**
 * The two vacation pickers of the settings dialog: whose vacations to overlay, chosen by group or by
 * user. Both reuse {@link EntityMultiAutocomplete} (`group/autosearch`, `vacation/users`). The chosen
 * chips are owned here because the backend answers a change with only `isFilterModified`, never the new
 * list — so the picks stay on screen without a round-trip. The change reports the bare id arrays the
 * `changeVacation*` endpoints expect.
 */
export function CalendarVacationSelects({
  groups,
  users,
  onGroupsChange,
  onUsersChange,
}: CalendarVacationSelectsProps) {
  const t = useTranslations();
  const [groupRefs, setGroupRefs] = useState<EntityRef[]>(() =>
    groups.map(toEntityRef)
  );
  const [userRefs, setUserRefs] = useState<EntityRef[]>(() =>
    users.map(toEntityRef)
  );

  const remove: EntityMultiAutocompleteProps["removeLabel"] = (entry) =>
    `${t("delete")}: ${entry.displayName}`;

  return (
    <div className="grid grid-cols-2 gap-x-4 gap-y-3">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="calendar-vacation-groups" className="text-sm">
          {t("calendar.filter.vacation.groups._")}
        </Label>
        <EntityMultiAutocomplete
          id="calendar-vacation-groups"
          url="group/autosearch?search=:search"
          value={groupRefs}
          onChange={(next) => {
            setGroupRefs(next);
            onGroupsChange(next.map((r) => r.id));
          }}
          removeLabel={remove}
          aria-label={t("calendar.filter.vacation.groups._")}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="calendar-vacation-users" className="text-sm">
          {t("calendar.filter.vacation.users._")}
        </Label>
        <EntityMultiAutocomplete
          id="calendar-vacation-users"
          url="vacation/users?search=:search"
          value={userRefs}
          onChange={(next) => {
            setUserRefs(next);
            onUsersChange(next.map((r) => r.id));
          }}
          removeLabel={remove}
          aria-label={t("calendar.filter.vacation.users._")}
        />
      </div>
    </div>
  );
}
