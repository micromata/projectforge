"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { PersonSelect } from "@/components/shared/person-select";
import { SHOW_INACTIVE_PARAM } from "@/hooks/use-show-inactive";
import { withMaxResults } from "@/lib/rs/autocomplete-url";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";
import {
  EMPTY_USER_EMAIL,
  type LanesAndPlanesUserEmail,
} from "../lanes-and-planes-config";
import { LanesAndPlanesRows } from "./lanes-and-planes-rows";

/** All users at once, deactivated ones included: the parameter stores ids only, the rows show the names. */
const ALL_USERS_URL = withMaxResults(
  `user/autosearch?${SHOW_INACTIVE_PARAM}=true&search=:search`,
  100_000
);

/**
 * The emails of the employees in Lanes & Planes (setting `userEmails`): a user, picked by searching and stored
 * by id, and the email Lanes & Planes knows the user by.
 */
export function LanesAndPlanesUserEmails({
  rows,
  invalid,
  onChange,
  onBlur,
}: {
  rows: LanesAndPlanesUserEmail[];
  invalid?: boolean;
  onChange: (rows: LanesAndPlanesUserEmail[]) => void;
  onBlur: () => void;
}) {
  const t = useTranslations("lanesAndPlanes.config");
  const users = useQuery({
    queryKey: ["lanesAndPlanes", "users"],
    queryFn: ({ signal }) =>
      fetchAutoCompletion<EntityRef>(ALL_USERS_URL, "", undefined, signal),
    enabled: rows.some((row) => row.userId != null),
    staleTime: 60_000,
  });
  const names = new Map(
    (users.data ?? []).map((user) => [user.id, user.displayName])
  );
  const refOf = (userId: number | null): EntityRef | null =>
    userId == null
      ? null
      : { id: userId, displayName: names.get(userId) ?? `#${userId}` };
  return (
    <LanesAndPlanesRows<LanesAndPlanesUserEmail>
      label={t("userEmails._")}
      hint={t("userEmails.tooltip")}
      invalid={invalid}
      ariaLabel={t("userEmails._")}
      rows={rows}
      empty={EMPTY_USER_EMAIL}
      onChange={onChange}
      renderInput={(value, set, ariaLabel) => (
        <div className="grid gap-2 md:grid-cols-[1fr_2fr]">
          <PersonSelect
            selectMe={false}
            value={refOf(value.userId)}
            aria-label={`${ariaLabel}: ${t("userEmails.user")}`}
            onChange={(user) => {
              set({ ...value, userId: user?.id ?? null });
              onBlur();
            }}
          />
          <Input
            value={value.email}
            type="email"
            placeholder={t("userEmails.email")}
            aria-label={`${ariaLabel}: ${t("userEmails.email")}`}
            onChange={(e) => set({ ...value, email: e.target.value })}
            onBlur={onBlur}
          />
        </div>
      )}
    />
  );
}
