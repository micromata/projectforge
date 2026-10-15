"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { SelectField } from "@/components/shared/form/select-field";
import { cn } from "@/lib/utils";
import { useUserRights } from "./use-user-rights";
import type { UserValues } from "./user-schema";

/**
 * The configurable rights of the user, below the groups they depend on: a box for a right that is
 * granted or not, a choice (read-only, partly, read-write) for any other. Which rights there are and
 * which values they offer is the backend's answer for the assigned groups (see useUserRights).
 */
export function UserRightsField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const infos = useUserRights();
  const rightIds = useStore(form.store, (s: unknown) =>
    (s as FormState).values.userRights.map((right) => right.rightId).join(",")
  );
  // The index of each right in the form's array, which is what the fields bind to.
  const indexes = new Map(rightIds.split(",").map((id, index) => [id, index]));
  if (infos.length === 0) {
    return (
      <p className={cn("text-sm text-muted-foreground", className)}>
        {t("user.rights.none")}
      </p>
    );
  }
  return (
    <div className={cn("grid gap-x-6 gap-y-3 sm:grid-cols-2", className)}>
      {infos.map((info) => {
        const index = indexes.get(info.rightId);
        // Not in the form yet: the answer for new groups arrived, the form follows a render later.
        if (index === undefined) return null;
        return info.booleanType ? (
          <CheckboxField
            key={info.rightId}
            name={`userRights[${index}].booleanValue`}
            label={info.label}
          />
        ) : (
          <SelectField
            key={info.rightId}
            name={`userRights[${index}].value`}
            label={info.label}
            options={info.values}
            clearable
            metadataLess
          />
        );
      })}
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<UserValues, "userRights">;
}
