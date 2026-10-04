"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { GROUP_METADATA } from "@/lib/metadata/group.generated";
import type { GroupValues } from "./group-schema";

/**
 * The "local group" flag, shown only where an external user management system (LDAP) is in use — without
 * one every group is local and the flag means nothing, so Wicket's GroupEditForm left it out.
 *
 * Whether that is the case is the backend's decision and travels with the entity
 * (`Group.externalUsermanagement`, set by `GroupEntityRest.transformFromDB`). A page-def field has no
 * `visible`, hence this custom field rather than a plain declaration.
 */
export function LocalGroupField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(GROUP_METADATA);
  const form = useEntityEditForm();
  const externalUsermanagement = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.externalUsermanagement
  );
  if (!externalUsermanagement) {
    return null;
  }
  return (
    <CheckboxField
      name="localGroup"
      label={label("localGroup")}
      hint={t("group.localGroup.tooltip")}
      className={className}
    />
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<GroupValues, "externalUsermanagement">;
}
