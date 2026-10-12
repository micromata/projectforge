"use client";

import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { NotificationRuleValues } from "./notification-rule-schema";

/** A live value of the rule form, for the fields shown depending on another one. */
export function useRuleValue<K extends keyof NotificationRuleValues>(
  name: K
): NotificationRuleValues[K] {
  const form = useEntityEditForm();
  return useStore(
    form.store,
    (s: unknown) => (s as { values: NotificationRuleValues }).values[name]
  );
}
