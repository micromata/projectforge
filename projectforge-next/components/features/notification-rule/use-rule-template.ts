"use client";

import { useEffect, useRef } from "react";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { useRuleTypes } from "./use-rule-types";
import { useRuleValue } from "./use-rule-value";

/**
 * A new rule starts with the texts of its type, if it has some (e.g. a rule of the CVE radar with a text that
 * names no details: the systems and CVEs are in the app, not in the mail): set when the type is chosen, as long as
 * subject and text are empty or still the template of the type chosen before. Only a change of the type sets it,
 * so the fields can be emptied afterwards.
 */
export function useRuleTemplate() {
  const types = useRuleTypes();
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
    const templateOf = (key: typeof ruleType) => {
      const type = types.find((info) => info.key === key);
      return type?.templateSubject || type?.templateText
        ? { subject: type.templateSubject ?? "", text: type.templateText ?? "" }
        : null;
    };
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
  }, [readOnly, id, ruleType, types, form]);
}
