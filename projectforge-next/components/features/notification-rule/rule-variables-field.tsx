"use client";

import { useState } from "react";
import {
  ArrowDown01Icon,
  ArrowRight01Icon,
  Copy01Icon,
  TickDouble01Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  fetchNotificationVariables,
  type NotificationVariable,
} from "@/lib/rs/notification";
import { useCopyToClipboard } from "@/hooks/use-copy-to-clipboard";
import { cn } from "@/lib/utils";
import { useRuleValue } from "./use-rule-value";

/**
 * The variables `{{key}}` usable in subject and text, the common ones (names of the recipient) and those
 * of the rule type, each copied (with the braces) by a click on it. The same query as RuleTextField's editor menu, so no
 * request of its own. With an example value each (how e.g. a list looks). Collapsed by default, the
 * header listing the names then. Shown read-only too.
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
  const [open, setOpen] = useState(false);
  return (
    <Collapsible asChild open={open} onOpenChange={setOpen}>
      <section
        aria-label={t("title")}
        className={cn(
          "rounded-md border border-primary/30 bg-primary/5 px-3 py-2 text-sm",
          className
        )}
      >
        <CollapsibleTrigger className="flex w-full items-baseline gap-1 text-left">
          <HugeiconsIcon
            icon={open ? ArrowDown01Icon : ArrowRight01Icon}
            size={14}
            className="shrink-0 self-center text-muted-foreground"
          />
          <h3 className="shrink-0 font-medium">{t("title")}</h3>
          {!open && variables && variables.length > 0 && (
            <span className="min-w-0 font-mono text-xs break-words text-muted-foreground">
              {variables.map((v) => v.key).join(", ")}
            </span>
          )}
        </CollapsibleTrigger>
        <CollapsibleContent>
          <p className="mt-1 text-muted-foreground">{t("info")}</p>
          <VariableGroup title={t("common")} variables={common} />
          <VariableGroup title={t("ruleType")} variables={specific} />
        </CollapsibleContent>
      </section>
    </Collapsible>
  );
}

function VariableGroup({
  title,
  variables,
}: {
  title: string;
  variables: NotificationVariable[];
}) {
  const t = useTranslations("notification.variables");
  if (variables.length === 0) return null;
  return (
    <div className="mt-2">
      <h4 className="text-xs font-medium text-muted-foreground uppercase">
        {title}
      </h4>
      <ul className="space-y-1">
        {variables.map((variable) => {
          const placeholder = `{{${variable.key}}}`;
          return (
            <li key={variable.key}>
              <div className="flex items-center gap-1">
                <CopyPlaceholder placeholder={placeholder} />
                <span className="truncate text-muted-foreground">
                  {variable.label}
                </span>
              </div>
              {variable.example && (
                <p className="pl-6 text-xs break-words text-muted-foreground">
                  {t("example")}: {variable.example}
                </p>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/** The placeholder `{{key}}` with a copy icon, the whole of it copying on click (the icon turns into a tick). */
function CopyPlaceholder({ placeholder }: { placeholder: string }) {
  const t = useTranslations();
  const { copied, copy } = useCopyToClipboard();
  return (
    <button
      type="button"
      aria-label={`${copied ? t("copied") : t("copy")}: ${placeholder}`}
      onClick={() => void copy(placeholder)}
      className="flex shrink-0 cursor-pointer items-center gap-1 rounded px-1 py-0.5 hover:bg-primary/10 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
    >
      <HugeiconsIcon
        icon={copied ? TickDouble01Icon : Copy01Icon}
        size={14}
        className="text-muted-foreground"
      />
      <code className="font-mono text-xs">{placeholder}</code>
    </button>
  );
}
