"use client";

import { useTranslations } from "next-intl";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useRuleValue } from "./use-rule-value";
import { useSmsInfo } from "./use-sms-info";

/**
 * The plain text of the SMS with variables, shown only if the cascade has an SMS step. Empty: the subject and
 * the link are sent. Cut to the max length of an SMS when sent.
 */
export function SmsTextField({ className }: { className?: string }) {
  const t = useTranslations("notification");
  const steps = useRuleValue("deliverySteps");
  const smsInfo = useSmsInfo();
  if (!steps.some((step) => step.channel === "SMS")) {
    return null;
  }
  return (
    <TextAreaField
      name="smsText"
      label={t("smsText")}
      hint={t("smsTextInfo", { arg0: smsInfo?.maxLength ?? 160 })}
      rows={3}
      className={className}
    />
  );
}
