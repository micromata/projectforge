"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
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
  fetchOrderNotificationSettings,
  ORDER_NOTIFICATION_QUERY_KEY,
  saveOrderNotificationSettings,
} from "@/lib/rs/my-account";
import { toast } from "@/lib/toast";
import type {
  OrderNotificationMode,
  OrderNotificationRule,
  OrderNotificationSettings,
} from "./types";

const MODES: OrderNotificationMode[] = ["IMMEDIATE", "DAILY", "MUTED"];

/**
 * How the user is notified by e-mail about changes of orders: a default for all orders and exceptions for
 * single projects or customers (the project wins over its customer). Saved at once with every change, like the
 * tokens and devices beside it — it is not part of the form's save.
 */
export function MyAccountOrderNotifications({
  canWrite,
}: {
  /** False for a restricted or demo user (a shared account): read-only. */
  canWrite: boolean;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [ruleType, setRuleType] =
    useState<OrderNotificationRule["type"]>("PROJECT");
  const query = useQuery({
    queryKey: ORDER_NOTIFICATION_QUERY_KEY,
    queryFn: ({ signal }) => fetchOrderNotificationSettings(signal),
  });
  if (query.isPending) {
    return <p className="text-sm text-muted-foreground">{t("loading")}</p>;
  }
  if (!query.data) {
    return (
      <p className="text-sm text-destructive">
        {t("access.exception.noAccess")}
      </p>
    );
  }
  const settings = query.data;
  const rules = settings.rules ?? [];
  const editable = canWrite && settings.writeAccess !== false && !busy;

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

  const save = async (next: OrderNotificationSettings) => {
    setBusy(true);
    try {
      const saved = await saveOrderNotificationSettings(next);
      queryClient.setQueryData(ORDER_NOTIFICATION_QUERY_KEY, saved);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    } finally {
      setBusy(false);
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
      <SelectTrigger className="w-56" aria-label={label}>
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
    void save({ ...settings, rules: next });
  };

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        {t("fibu.auftrag.notification.prefs.description")}
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <span className="w-40 text-sm font-medium">
          {t("fibu.auftrag.notification.prefs.default")}
        </span>
        {modeSelect(
          settings.defaultMode,
          (defaultMode) => void save({ ...settings, defaultMode }),
          t("fibu.auftrag.notification.prefs.default")
        )}
      </div>
      <h3 className="text-sm font-medium">
        {t("fibu.auftrag.notification.prefs.rules")}
      </h3>
      {rules.map((rule, index) => (
        <div
          key={`${rule.type}-${rule.id}`}
          className="flex flex-wrap items-center gap-2 rounded-md border p-2"
        >
          <span className="w-40 text-sm text-muted-foreground">
            {typeLabel(rule.type)}
          </span>
          <span className="min-w-40 flex-1 text-sm">{rule.displayName}</span>
          {modeSelect(
            rule.mode,
            (mode) => changeRule(index, { ...rule, mode }),
            typeLabel(rule.type)
          )}
          <Button
            variant="ghost"
            size="icon"
            disabled={!editable}
            aria-label={t("delete")}
            onClick={() => changeRule(index, null)}
          >
            <HugeiconsIcon icon={Delete02Icon} />
          </Button>
        </div>
      ))}
      {canWrite && (
        <div className="flex flex-wrap items-center gap-2">
          <Select
            value={ruleType}
            disabled={!editable}
            onValueChange={(type) =>
              setRuleType(type as OrderNotificationRule["type"])
            }
          >
            <SelectTrigger
              className="w-40"
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
              void save({
                ...settings,
                rules: [
                  ...rules,
                  { type: ruleType, id: picked.id, mode: "DAILY" },
                ],
              });
            }}
          />
        </div>
      )}
    </div>
  );
}
