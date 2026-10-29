"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import {
  type OrderNotificationMode,
  type OrderNotificationRule,
  type OrderNotificationSettings,
} from "@/lib/rs/order-notification";

const MODES: OrderNotificationMode[] = ["IMMEDIATE", "DAILY", "MUTED"];

/**
 * How the user is notified by e-mail about changes of orders: a default for all orders and exceptions for
 * single projects or customers. Works on a draft held by the caller ([OrderNotificationDialog]), which saves
 * it or throws it away.
 */
export function OrderNotificationsEditor({
  settings,
  onChange: save,
  editable,
  suggestions = [],
}: {
  settings: OrderNotificationSettings;
  onChange: (settings: OrderNotificationSettings) => void;
  editable: boolean;
  /**
   * The project and customer of the order at hand: each one without a rule yet is offered as a link that adds
   * it as an exception.
   */
  suggestions?: Pick<OrderNotificationRule, "type" | "id" | "displayName">[];
}) {
  const t = useTranslations();
  const [ruleType, setRuleType] =
    useState<OrderNotificationRule["type"]>("PROJECT");
  const rules = settings.rules ?? [];

  const modeLabel = (mode: OrderNotificationMode) => {
    switch (mode) {
      case "IMMEDIATE":
        return t("fibu.auftrag.notification.mode.immediate");
      case "DAILY":
        return t("fibu.auftrag.notification.mode.daily");
      case "MUTED":
        return t("fibu.auftrag.notification.mode.muted");
    }
  };

  const modeSelect = (
    value: OrderNotificationMode,
    onChange: (mode: OrderNotificationMode) => void,
    label: string
  ) => (
    <Select
      value={value}
      disabled={!editable}
      onValueChange={(mode) => onChange(mode as OrderNotificationMode)}
    >
      <SelectTrigger size="sm" className="w-52" aria-label={label}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {MODES.map((mode) => (
          <SelectItem key={mode} value={mode}>
            {modeLabel(mode)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );

  const typeLabel = (type: OrderNotificationRule["type"]) =>
    type === "PROJECT"
      ? t("fibu.auftrag.notification.prefs.project")
      : t("fibu.auftrag.notification.prefs.customer");

  const changeRule = (index: number, rule: OrderNotificationRule | null) => {
    const next = rules.filter((_, i) => i !== index);
    if (rule) next.splice(index, 0, rule);
    save({ ...settings, rules: next });
  };

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs text-muted-foreground">
        {t("fibu.auftrag.notification.prefs.description")}
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <span className="w-24 text-sm font-medium">
          {t("fibu.auftrag.notification.prefs.default")}
        </span>
        {modeSelect(
          settings.defaultMode,
          (defaultMode) => save({ ...settings, defaultMode }),
          t("fibu.auftrag.notification.prefs.default")
        )}
      </div>
      <h3 className="text-sm font-medium">
        {t("fibu.auftrag.notification.prefs.rules")}
      </h3>
      {rules.map((rule, index) => (
        <div
          key={`${rule.type}-${rule.id}`}
          className="flex items-center gap-2 border-b py-1"
        >
          <span className="w-16 shrink-0 text-xs text-muted-foreground">
            {typeLabel(rule.type)}
          </span>
          <span className="min-w-0 flex-1 text-sm leading-tight">
            {rule.displayName}
          </span>
          {modeSelect(
            rule.mode,
            (mode) => changeRule(index, { ...rule, mode }),
            typeLabel(rule.type)
          )}
          <Button
            variant="ghost"
            size="icon"
            className="size-7 shrink-0"
            disabled={!editable}
            aria-label={t("delete")}
            onClick={() => changeRule(index, null)}
          >
            <HugeiconsIcon icon={Delete02Icon} />
          </Button>
        </div>
      ))}
      {editable &&
        suggestions
          .filter((s) => !rules.some((r) => r.type === s.type && r.id === s.id))
          .map((s) => (
            <Button
              key={`${s.type}-${s.id}`}
              variant="link"
              className="h-auto self-start p-0"
              onClick={() =>
                save({
                  ...settings,
                  rules: [...rules, { ...s, mode: "DAILY" }],
                })
              }
            >
              {t("fibu.auftrag.notification.prefs.adopt", {
                arg0: typeLabel(s.type),
                arg1: s.displayName ?? s.id,
              })}
            </Button>
          ))}
      {editable && (
        <div className="flex flex-wrap items-center gap-2">
          <Select
            value={ruleType}
            onValueChange={(type) =>
              setRuleType(type as OrderNotificationRule["type"])
            }
          >
            <SelectTrigger
              size="sm"
              className="w-28"
              aria-label={t("fibu.auftrag.notification.prefs.rules")}
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PROJECT">{typeLabel("PROJECT")}</SelectItem>
              <SelectItem value="CUSTOMER">{typeLabel("CUSTOMER")}</SelectItem>
            </SelectContent>
          </Select>
          <EntityAutocomplete
            key={ruleType}
            url={`${ruleType === "PROJECT" ? "project" : "customer"}/autosearch?search=:search`}
            value={null}
            disabled={!editable}
            aria-label={typeLabel(ruleType)}
            onChange={(picked) => {
              if (
                !picked ||
                rules.some((r) => r.type === ruleType && r.id === picked.id)
              ) {
                return;
              }
              save({
                ...settings,
                rules: [
                  ...rules,
                  {
                    type: ruleType,
                    id: picked.id,
                    displayName: picked.displayName,
                    mode: "DAILY",
                  },
                ],
              });
            }}
          />
        </div>
      )}
    </div>
  );
}
