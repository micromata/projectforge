"use client";

import { useQuery } from "@tanstack/react-query";
import {
  fetchNotificationRuleTypes,
  type NotificationRuleTypeInfo,
} from "@/lib/rs/notification";
import { useRuleValue } from "./use-rule-value";

/** The registered rule types, in the order of the editor (empty while loading). */
export function useRuleTypes(): NotificationRuleTypeInfo[] {
  const { data } = useQuery({
    queryKey: ["notificationRule", "ruleTypes"],
    queryFn: ({ signal }) => fetchNotificationRuleTypes(signal),
    staleTime: Infinity,
  });
  return data ?? [];
}

/** The description of the rule type chosen in the form, undefined while loading or if none is chosen. */
export function useChosenRuleType(): NotificationRuleTypeInfo | undefined {
  const ruleType = useRuleValue("ruleType");
  return useRuleTypes().find((type) => type.key === ruleType);
}
