"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { RichText } from "@/components/shared/rich-text";
import { RichTextEditor } from "@/components/shared/rich-text-editor";
import { fetchNotificationVariables } from "@/lib/rs/notification";
import { useRuleValue } from "./use-rule-value";

/**
 * The text of the notifications (in the app and the mail), with the variables `{{key}}` of the rule
 * type offered by the editor's menu.
 */
export function RuleTextField({ className }: { className?: string }) {
  const t = useTranslations("notification");
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const ruleType = useRuleValue("ruleType");
  const { data: variables } = useQuery({
    queryKey: ["notificationRule", "variables", ruleType],
    queryFn: ({ signal }) => fetchNotificationVariables(ruleType, signal),
    staleTime: Infinity,
  });
  const label = t("text");
  return (
    <form.Field name={"text" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        return (
          <FieldShell
            name="text"
            label={label}
            required
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            {readOnly ? (
              <RichText html={field.state.value ?? ""} />
            ) : (
              <RichTextEditor
                id={ids.controlId}
                value={field.state.value}
                onChange={field.handleChange}
                onBlur={field.handleBlur}
                variables={variables}
                invalid={invalid}
              />
            )}
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
