"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { NotificationChannel } from "@/lib/rs/notification";
import type { DeliveryStep } from "./types";

type Unit = "minutes" | "hours" | "days";

const MINUTES: Record<Unit, number> = { minutes: 1, hours: 60, days: 1440 };

/** The largest unit the delay is a whole multiple of: 2880 minutes are shown as 2 days. */
function unitOf(minutes: number): Unit {
  if (minutes > 0 && minutes % MINUTES.days === 0) return "days";
  if (minutes > 0 && minutes % MINUTES.hours === 0) return "hours";
  return "minutes";
}

/** One step of the delivery cascade: the channel, after how long, and whether only if unconfirmed. */
export function DeliveryStepRow({
  step,
  index,
  disabled,
  onChange,
  onRemove,
}: {
  step: DeliveryStep;
  index: number;
  disabled: boolean;
  onChange: (step: DeliveryStep) => void;
  onRemove: () => void;
}) {
  const t = useTranslations("notification");
  // The unit is the user's choice while editing (1 day typed as "24 hours" stays in hours).
  const [unit, setUnit] = useState<Unit>(() => unitOf(step.delayMinutes));
  const amount = Math.round(step.delayMinutes / MINUTES[unit]);
  const id = `delivery-step-${index}`;
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-md border p-2">
      <Select
        value={step.channel}
        disabled={disabled}
        onValueChange={(channel) =>
          onChange({ ...step, channel: channel as NotificationChannel })
        }
      >
        <SelectTrigger className="w-44" aria-label={t("delivery.channel")}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="IN_APP">{t("channels.IN_APP")}</SelectItem>
          <SelectItem value="MAIL">{t("channels.MAIL")}</SelectItem>
          <SelectItem value="SMS">{t("channels.SMS")}</SelectItem>
        </SelectContent>
      </Select>
      <span className="text-sm text-muted-foreground">
        {t("delivery.delay")}
      </span>
      <Input
        type="number"
        min={0}
        step={1}
        value={amount}
        disabled={disabled}
        className="w-20"
        aria-label={t("delivery.delay")}
        onChange={(event) => {
          const typed = Math.max(0, Math.floor(Number(event.target.value)));
          onChange({
            ...step,
            delayMinutes: (Number.isFinite(typed) ? typed : 0) * MINUTES[unit],
          });
        }}
      />
      <Select
        value={unit}
        disabled={disabled}
        onValueChange={(next) => {
          setUnit(next as Unit);
          onChange({ ...step, delayMinutes: amount * MINUTES[next as Unit] });
        }}
      >
        <SelectTrigger className="w-28" aria-label={t("delivery.delay")}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="minutes">{t("delivery.unit.minutes")}</SelectItem>
          <SelectItem value="hours">{t("delivery.unit.hours")}</SelectItem>
          <SelectItem value="days">{t("delivery.unit.days")}</SelectItem>
        </SelectContent>
      </Select>
      <div className="flex items-center gap-2">
        <Checkbox
          id={`${id}-unack`}
          checked={step.onlyIfUnacknowledged}
          disabled={disabled}
          onCheckedChange={(checked) =>
            onChange({ ...step, onlyIfUnacknowledged: checked === true })
          }
        />
        <Label htmlFor={`${id}-unack`} className="text-xs font-normal">
          {t("delivery.onlyIfUnacknowledged")}
        </Label>
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="ml-auto"
        disabled={disabled}
        aria-label={t("delivery.removeStep")}
        onClick={onRemove}
      >
        <HugeiconsIcon icon={Delete02Icon} size={14} aria-hidden />
      </Button>
    </div>
  );
}
