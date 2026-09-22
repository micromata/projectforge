"use client";

import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ColorSwatchPopover } from "@/components/shared/color-swatch-popover";
import { MarkdownText } from "@/components/shared/markdown-text";
import type {
  CalendarEventColorScheme,
  CalendarSettings,
} from "@/lib/rs/calendar-types";
import { useCalendarSettings } from "./use-calendar-settings";

const COLOR_SCHEMES: CalendarEventColorScheme[] = ["STANDARD", "CLASSIC"];

/** The four colour fields, keyed by their `CalendarSettings` property and their existing i18n label. */
const COLOR_FIELDS: {
  key: keyof CalendarSettings;
  label: string;
}[] = [
  { key: "timesheetsColor", label: "calendar.settings.colors.timesheets" },
  {
    key: "timesheetsStatsColor",
    label: "calendar.settings.colors.timesheetStats",
  },
  {
    key: "timesheetsBreaksColor",
    label: "calendar.settings.colors.timesheetBreaks",
  },
  { key: "vacationsColor", label: "calendar.settings.colors.vacations._" },
];

/**
 * The colour section of the gear dialog: the four event colours, the colour scheme and the
 * alternate-hours toggle. Each control persists the whole settings the moment it changes (see
 * {@link useCalendarSettings}) — no draft, no save button, matching the rest of the dialog. Rendered
 * only while the dialog is open, so its own settings query is naturally gated to that.
 */
export function CalendarColorSettings() {
  const t = useTranslations();
  const { settings, isLoading, change } = useCalendarSettings();

  return (
    <div className="flex flex-col gap-3">
      <Label className="text-sm font-medium">
        {t("calendar.settings.colors._")}
      </Label>
      <MarkdownText
        text={t("calendar.settings.intro")}
        className="text-xs text-muted-foreground"
      />

      <div className="grid grid-cols-2 gap-x-4 gap-y-2" aria-busy={isLoading}>
        {COLOR_FIELDS.map((field) => (
          <div key={field.key} className="flex items-center gap-2">
            <ColorSwatchPopover
              value={(settings?.[field.key] as string | null | undefined) ?? ""}
              onChange={(value) => change({ [field.key]: value })}
              aria-label={t(field.label)}
            />
            <Label className="truncate text-sm font-normal">
              {t(field.label)}
            </Label>
          </div>
        ))}
      </div>

      <Alert>
        <AlertDescription>
          <MarkdownText text={t("calendar.settings.colors.vacations.info")} />
        </AlertDescription>
      </Alert>

      <div className="grid grid-cols-2 items-end gap-x-4 gap-y-2">
        <div className="flex flex-col gap-1.5">
          <Label className="text-sm">
            {t("calendar.settings.colors.scheme._")}
          </Label>
          <Select
            value={settings?.colorScheme ?? "STANDARD"}
            onValueChange={(value) =>
              change({ colorScheme: value as CalendarEventColorScheme })
            }
          >
            <SelectTrigger className="h-8">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {COLOR_SCHEMES.map((scheme) => (
                <SelectItem key={scheme} value={scheme}>
                  {t(`calendar.settings.colors.scheme.${scheme.toLowerCase()}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex h-8 items-center gap-2">
          <Checkbox
            id="calendar-alternate-hours"
            checked={settings?.alternateHoursBackground ?? false}
            onCheckedChange={(value) =>
              change({ alternateHoursBackground: value === true })
            }
          />
          <Label
            htmlFor="calendar-alternate-hours"
            className="text-sm font-normal"
          >
            {t("calendar.settings.alternateHoursBackground._")}
          </Label>
        </div>
      </div>
    </div>
  );
}
