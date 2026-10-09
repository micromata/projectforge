"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { SelectField } from "@/components/shared/form/select-field";
import { fetchMenuBadgeOptions } from "@/lib/rs/notification";

/** The menu entry counting the open notifications of the rule, chosen from the entries of the menu. */
export function MenuBadgeField({ className }: { className?: string }) {
  const t = useTranslations("notification.rule");
  const { data } = useQuery({
    queryKey: ["notificationRule", "menuBadges"],
    queryFn: ({ signal }) => fetchMenuBadgeOptions(signal),
    staleTime: Infinity,
  });
  return (
    <SelectField
      name="menuBadge"
      label={t("menuBadge")}
      clearable
      className={className}
      options={(data ?? []).map((option) => ({
        value: option.id,
        label: option.label,
      }))}
    />
  );
}
