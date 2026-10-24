"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { CopyButton } from "@/components/shared/copy-button";
import {
  fetchNotificationVariables,
  type NotificationVariable,
} from "@/lib/rs/notification";
import { cn } from "@/lib/utils";
import { useRuleValue } from "./use-rule-value";

/**
 * The variables `{{key}}` usable in subject and text, the common ones (names of the recipient) and those
 * of the rule type, each with a button copying it. The same query as RuleTextField's editor menu, so no
 * request of its own. Shown read-only too.
 */
export function RuleVariablesField({ className }: { className?: string }) {
  const t = useTranslations("notification.variables");
  const ruleType = useRuleValue("ruleType");
  const { data: variables } = useQuery({
    queryKey: ["notificationRule", "variables", ruleType],
    queryFn: ({ signal }) => fetchNotificationVariables(ruleType, signal),
    staleTime: Infinity,
  });
  const common = variables?.filter((v) => v.common) ?? [];
  const specific = variables?.filter((v) => !v.common) ?? [];
  return (
    <section
      aria-label={t("title")}
      className={cn(
        "rounded-md border border-primary/30 bg-primary/5 px-3 py-2 text-sm",
        className
      )}
    >
      <h3 className="font-medium">{t("title")}</h3>
      <p className="text-muted-foreground">{t("info")}</p>
      <VariableGroup title={t("common")} variables={common} />
      <VariableGroup title={t("ruleType")} variables={specific} />
    </section>
  );
}

function VariableGroup({
  title,
  variables,
}: {
  title: string;
  variables: NotificationVariable[];
}) {
  if (variables.length === 0) return null;
  return (
    <div className="mt-2">
      <h4 className="text-xs font-medium text-muted-foreground uppercase">
        {title}
      </h4>
      <ul className="grid gap-x-4 sm:grid-cols-2">
        {variables.map((variable) => {
          const placeholder = `{{${variable.key}}}`;
          return (
            <li key={variable.key} className="flex items-center gap-1">
              <CopyButton
                value={placeholder}
                label={placeholder}
                variant="ghost"
                iconSize={14}
              />
              <code className="font-mono text-xs">{placeholder}</code>
              <span className="truncate text-muted-foreground">
                {variable.label}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
