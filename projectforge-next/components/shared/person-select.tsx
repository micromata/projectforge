"use client";

import { useSelectMeRef } from "@/hooks/use-current-user-ref";
import {
  EntityAutocomplete,
  type EntityAutocompleteProps,
} from "./entity-autocomplete";

export interface PersonSelectProps extends Omit<
  EntityAutocompleteProps,
  "url" | "selectMe"
> {
  /** Whom to pick: a user (`PFUserDO`) or an employee (`EmployeeDO`), searched in its own autosearch. */
  kind?: "user" | "employee";
  /** Leaves out the „select me" smiley, where picking oneself makes no sense. */
  selectMe?: false;
}

/**
 * Picks a user or an employee outside a form — the „select me" smiley included, as the legacy
 * UserSelect.jsx offers it. Inside a form the same is [EntityAutocompleteField] with `entity="user"` or
 * `"employee"`.
 */
export function PersonSelect({
  kind = "user",
  selectMe,
  ...props
}: PersonSelectProps) {
  const me = useSelectMeRef(kind);
  return (
    <EntityAutocomplete
      {...props}
      url={`${kind}/autosearch?search=:search`}
      selectMe={selectMe === false ? null : me}
    />
  );
}
