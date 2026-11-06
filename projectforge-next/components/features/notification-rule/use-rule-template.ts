"use client";

import { useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { useRuleValue } from "./use-rule-value";

/**
 * A new rule of the CVE radar starts with a text that names no details (the systems and CVEs are in the app, not
 * in the mail): set when the type is chosen, as long as subject and text are empty or still the template of the
 * type chosen before. Only a change of the type sets it, so the fields can be emptied afterwards.
 */
export function useRuleTemplate() {
  const t = useTranslations("notification.template");
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const ruleType = useRuleValue("ruleType");
  const id = useRuleValue("id");
  const previous = useRef(ruleType);
  useEffect(() => {
    if (previous.current === ruleType) return;
    const before = previous.current;
    previous.current = ruleType;
    if (readOnly || id != null) return;
    // The variables `{{key}}` are written as `[key]` in the texts (braces are ICU syntax).
    const variables = (value: string) => value.replace(/\[(\w+)]/g, "{{$1}}");
    const templateOf = (type: typeof ruleType) =>
      type === "CVERADAR_FINDINGS"
        ? {
            subject: t("findingsSubject"),
            text: variables(t("findingsText")),
          }
        : type === "CVERADAR_STALE_LISTS"
          ? {
              subject: t("staleListsSubject"),
              text: variables(t("staleListsText")),
            }
          : type === "CVERADAR_TEAM_CHECK_UNANSWERED"
            ? {
                subject: t("teamCheckUnansweredSubject"),
                text: variables(t("teamCheckUnansweredText")),
              }
            : null;
    const now = {
      subject: form.getFieldValue("subject" as never) as string | null,
      text: form.getFieldValue("text" as never) as string | null,
    };
    const old = templateOf(before);
    const untouched =
      (!now.subject?.trim() && !now.text?.trim()) ||
      (old != null && now.subject === old.subject && now.text === old.text);
    if (!untouched) return;
    const next = templateOf(ruleType);
    form.setFieldValue("subject" as never, (next?.subject ?? "") as never);
    form.setFieldValue("text" as never, (next?.text ?? "") as never);
  }, [readOnly, id, ruleType, t, form]);
}
