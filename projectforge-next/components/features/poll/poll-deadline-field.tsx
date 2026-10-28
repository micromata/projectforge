"use client";

import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { InputField } from "@/components/shared/form/input-field";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";

/**
 * The deadline of a poll: mandatory, but not for a template, which is never answered and has none. The metadata
 * can't tell (the column is optional for the templates' sake), `PollEntityRest.validate` decides.
 */
export function PollDeadlineField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const template = useStore(form.store, (s: any) => s.values.template === true);
  return (
    <InputField
      name="deadline"
      type="date"
      label={t("poll.deadline._")}
      hint={t("poll.deadline.tooltip")}
      required={!template}
      disabled={readOnly}
      className={className}
    />
  );
}
