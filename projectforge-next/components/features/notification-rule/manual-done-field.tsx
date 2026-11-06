"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useChosenRuleType } from "./use-rule-types";
import { useRuleValue } from "./use-rule-value";

/**
 * May the recipients mark a notification as done? Forced where the rule type says so, e.g. a manual rule
 * with banner: without a condition nothing resolves the banner, so it would stay forever (the backend
 * forces it, too: NotificationRuleHandler.isManualDoneForced).
 */
export function ManualDoneField({ className }: { className?: string }) {
  const t = useTranslations("notification.rule");
  const form = useEntityEditForm();
  const ruleType = useChosenRuleType();
  const display = useRuleValue("display");
  const forced =
    display != null && ruleType?.manualDoneForced.includes(display) === true;
  const manualDone = useRuleValue("manualDone");
  useEffect(() => {
    if (forced && manualDone !== true) {
      form.setFieldValue("manualDone" as never, true as never);
    }
  }, [forced, manualDone, form]);
  return (
    <CheckboxField
      name="manualDone"
      label={t("manualDone")}
      hint={forced ? t("manualDoneForced") : undefined}
      disabled={forced}
      className={className}
    />
  );
}
