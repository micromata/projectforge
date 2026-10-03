"use client";

import { useState } from "react";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";

/**
 * The value of a mass-update field pointing at another entity — a user or a group — picked by searching
 * for it (`{type}/autosearch`).
 *
 * The parameter carries the id only (`MassUpdateParameter.id`, what the backend resolves), so the picked
 * entry's name is kept here for the trigger. It is shown only while it still is the parameter's value: a
 * mode switch that drops the id empties the control as well.
 */
export function MassUpdateEntityControl({
  url,
  id,
  withSelectMe,
  label,
  onChange,
}: {
  /** The lookup url with its literal `:search` placeholder. */
  url: string;
  id: number | null;
  /** Offer the logged-in user with one click (for a user field). */
  withSelectMe?: boolean;
  label: string;
  onChange: (id: number | undefined) => void;
}) {
  const currentUser = useCurrentUserRef();
  const [picked, setPicked] = useState<EntityRef | null>(null);
  const value = picked != null && picked.id === id ? picked : null;
  return (
    <EntityAutocomplete
      url={url}
      value={value}
      onChange={(entry) => {
        setPicked(
          entry ? { id: entry.id, displayName: entry.displayName } : null
        );
        onChange(entry?.id ?? undefined);
      }}
      selectMe={withSelectMe ? currentUser : undefined}
      aria-label={label}
    />
  );
}
