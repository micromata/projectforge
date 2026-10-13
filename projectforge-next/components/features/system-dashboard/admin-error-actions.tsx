"use client";

import { useTranslations } from "next-intl";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import type {
  LogGroupAction,
  LogGroupDetail,
  LogGroupUpdate,
  LogNotify,
} from "@/lib/rs/admin-errors";
import { NOTIFY_KEYS } from "./admin-errors-labels";

/** The notify select's value for "no override": the event's own rule. */
const EVENT_NOTIFY = "EVENT";

const NOTIFY_OPTIONS: LogNotify[] = [
  "NONE",
  "DIGEST_IF_NEW",
  "DIGEST",
  "IMMEDIATE",
];

/**
 * The status actions of a problem, offered as far as they change something: acknowledge a new one, resolve,
 * ignore, reopen, mute for a while, and override the notify rule of its event.
 */
export function AdminErrorActions({
  detail,
  pending,
  onChange,
}: {
  detail: LogGroupDetail;
  pending: boolean;
  onChange: (change: Omit<LogGroupUpdate, "ids">) => void;
}) {
  const t = useTranslations();
  const entry = detail.entry;
  const button = (
    label: string,
    action: LogGroupAction,
    muteDays?: number,
    primary?: boolean
  ) => (
    <Button
      key={`${action}-${muteDays ?? ""}`}
      size="sm"
      variant={primary ? "default" : "outline"}
      disabled={pending}
      onClick={() => onChange({ action, muteDays })}
    >
      {label}
    </Button>
  );
  return (
    <div className="flex flex-wrap items-center gap-2">
      {entry.status === "NEW" &&
        button(
          t("system.admin.adminErrors.action.acknowledge"),
          "ACKNOWLEDGE",
          undefined,
          true
        )}
      {entry.status !== "RESOLVED" &&
        button(t("system.admin.adminErrors.action.resolve"), "RESOLVE")}
      {entry.status !== "IGNORED" &&
        button(t("system.admin.adminErrors.action.ignore"), "IGNORE")}
      {entry.status !== "NEW" &&
        button(t("system.admin.adminErrors.action.reopen"), "REOPEN")}
      {entry.mutedUntil != null
        ? button(t("system.admin.adminErrors.action.unmute"), "UNMUTE")
        : [
            button(t("system.admin.adminErrors.action.mute.day"), "MUTE", 1),
            ...[7, 30].map((days) =>
              button(
                t("system.admin.adminErrors.action.mute.days", { arg0: days }),
                "MUTE",
                days
              )
            ),
          ]}
      <div className="flex items-center gap-2">
        <Label htmlFor="admin-error-notify">
          {t("system.admin.adminErrors.notify._")}
        </Label>
        <Select
          value={entry.overrideNotify ?? EVENT_NOTIFY}
          disabled={pending}
          onValueChange={(value) =>
            onChange({
              action: "SET_NOTIFY",
              notify: value === EVENT_NOTIFY ? null : (value as LogNotify),
            })
          }
        >
          <SelectTrigger id="admin-error-notify" className="h-8 w-64">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={EVENT_NOTIFY}>
              {t("system.admin.adminErrors.notify.default", {
                arg0: t(NOTIFY_KEYS[detail.eventNotify]),
              })}
            </SelectItem>
            {NOTIFY_OPTIONS.map((notify) => (
              <SelectItem key={notify} value={notify}>
                {t(NOTIFY_KEYS[notify])}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
