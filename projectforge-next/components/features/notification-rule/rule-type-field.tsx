"use client";

import { useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { SelectField } from "@/components/shared/form/select-field";
import { useRuleTypes } from "./use-rule-types";
import { useRuleValue } from "./use-rule-value";

/**
 * The rule type, one of those the backend registered (NotificationRuleRegistry). A change gives the params
 * of the new type their defaults, keeping the values of params of the same key.
 */
export function RuleTypeField({ className }: { className?: string }) {
  const t = useTranslations("notification.rule");
  const types = useRuleTypes();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const ruleType = useRuleValue("ruleType");
  const previous = useRef(ruleType);
  useEffect(() => {
    const info = types.find((type) => type.key === ruleType);
    if (previous.current === ruleType || readOnly || !info) return;
    previous.current = ruleType;
    const current = (form.getFieldValue("params" as never) ?? {}) as Record<
      string,
      unknown
    >;
    const params = Object.fromEntries(
      info.params.map((param) => [
        param.key,
        current[param.key] ?? param.default ?? null,
      ])
    );
    form.setFieldValue("params" as never, params as never);
  }, [ruleType, types, readOnly, form]);
  return (
    <SelectField
      name="ruleType"
      label={t("ruleType")}
      clearable={false}
      className={className}
      options={[
        ...types.map((type) => ({ value: type.key, label: type.label })),
        // A stored type no handler is registered for (any more): shown by its key, refused on save.
        ...(ruleType &&
        types.length > 0 &&
        !types.some((type) => type.key === ruleType)
          ? [{ value: ruleType, label: ruleType }]
          : []),
      ]}
    />
  );
}
