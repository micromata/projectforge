"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Settings02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import {
  CALENDAR_GRID_SIZES,
  type CalendarInit,
} from "@/lib/rs/calendar-types";
import type { useCalendarFilterMutations } from "./use-calendar-filter-mutations";
import { CalendarVacationSelects } from "./calendar-vacation-selects";
import { CalendarColorSettings } from "./calendar-color-settings";

const HOURS = Array.from({ length: 24 }, (_, i) => i);

interface CalendarSettingsDialogProps {
  init: CalendarInit;
  mutations: ReturnType<typeof useCalendarFilterMutations>;
}

/**
 * The gear dialog: the presentational settings that do not belong on the toolbar itself — default
 * calendar, whose timesheets to show, breaks, vacation overlays, the time-grid's slot size and first
 * hour, and the event colours (see CalendarColorSettings; formerly the separate `calendarSettings`
 * page). Each control fires its own `change*`/persist the moment it changes; the dialog holds no draft,
 * so there is no save button, exactly as the legacy settings modal.
 */
export function CalendarSettingsDialog({
  init,
  mutations,
}: CalendarSettingsDialogProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const filter = init.filter;
  const otherEnabled = filter?.otherTimesheetUsersEnabled ?? false;
  const timesheetUser = init.timesheetUser;
  const showsOwn = (timesheetUser?.id ?? 0) > 0;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t("calendar.view.settings.tooltip")}
          className="size-8"
        >
          <HugeiconsIcon icon={Settings02Icon} size={16} />
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("calendar.settings._")}</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-3">
          <div className="grid grid-cols-2 items-end gap-x-4 gap-y-3">
            <div className="flex flex-col gap-1.5">
              <Label className="text-sm">{t("calendar.defaultCalendar")}</Label>
              <Select
                value={filter?.defaultCalendarId?.toString() ?? ""}
                onValueChange={(value) =>
                  mutations.changeDefaultCalendar(Number(value))
                }
              >
                <SelectTrigger className="h-8">
                  <SelectValue placeholder={t("select._")} />
                </SelectTrigger>
                <SelectContent>
                  {(init.listOfDefaultCalendars ?? []).map((calendar) => (
                    <SelectItem
                      key={calendar.id}
                      value={String(calendar.id ?? "")}
                    >
                      {calendar.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {otherEnabled ? (
              <div className="flex flex-col gap-1.5">
                <Label className="text-sm">
                  {t("calendar.option.timesheets")}
                </Label>
                <EntityAutocomplete
                  url="user/autosearch?search=:search"
                  value={
                    showsOwn && timesheetUser?.id != null
                      ? {
                          id: timesheetUser.id,
                          displayName: timesheetUser.displayName ?? "",
                        }
                      : null
                  }
                  onChange={(user) => mutations.changeTimesheetUser(user?.id)}
                  aria-label={t("calendar.option.timesheets")}
                />
              </div>
            ) : (
              <div className="flex h-8 items-center gap-2">
                <Checkbox
                  id="calendar-show-timesheets"
                  checked={showsOwn}
                  // The legacy sentinel: a positive id shows the timesheets, -1 hides them.
                  onCheckedChange={(value) =>
                    mutations.changeTimesheetUser(value === true ? 1 : -1)
                  }
                />
                <Label
                  htmlFor="calendar-show-timesheets"
                  className="text-sm font-normal"
                >
                  {t("calendar.option.timesheets")}
                </Label>
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <Checkbox
              id="calendar-show-breaks"
              checked={filter?.showBreaks ?? false}
              onCheckedChange={(value) =>
                mutations.changeShowBreaks(value === true)
              }
            />
            <Label
              htmlFor="calendar-show-breaks"
              className="text-sm font-normal"
            >
              {t("calendar.option.showBreaks")}
            </Label>
          </div>

          {open && (
            <CalendarVacationSelects
              groups={init.vacationGroups ?? []}
              users={init.vacationUsers ?? []}
              onGroupsChange={mutations.changeVacationGroups}
              onUsersChange={mutations.changeVacationUsers}
            />
          )}

          <div className="grid grid-cols-2 gap-x-4 gap-y-3">
            <div className="flex flex-col gap-1.5">
              <Label className="text-sm">{t("calendar.option.gridSize")}</Label>
              <Select
                value={filter?.gridSize?.toString() ?? "30"}
                onValueChange={(value) =>
                  mutations.changeGridSize(Number(value))
                }
              >
                <SelectTrigger className="h-8">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CALENDAR_GRID_SIZES.map((size) => (
                    <SelectItem key={size} value={String(size)}>
                      {size}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label className="text-sm">
                {t("calendar.option.firstHour")}
              </Label>
              <Select
                value={filter?.firstHour?.toString() ?? "8"}
                onValueChange={(value) =>
                  mutations.changeFirstHour(Number(value))
                }
              >
                <SelectTrigger className="h-8">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {HOURS.map((hour) => (
                    <SelectItem key={hour} value={String(hour)}>
                      {String(hour).padStart(2, "0")}:00
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {open && (
            <>
              <Separator />
              <CalendarColorSettings />
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
