"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserGroupIcon } from "@hugeicons/core-free-icons";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { GuardedLink } from "@/components/shared/guarded-link";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  fetchUserAvailabilityView,
  VACATION_EXPORT_USERS_URL,
} from "@/lib/rs/vacation-export";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import { AvailabilityQuickViewChart } from "./availability-quick-view-chart";

/** The periods offered, in weeks from the beginning of this week; the backend shows 12 at most. */
const WEEKS = [4, 8, 12] as const;
const DEFAULT_WEEKS = 8;

/**
 * A quick look at whether colleagues are available: pick one or more users (only those the team vacation calendar
 * would offer, freelancers included; shown as chips) and see their vacations and availability entries of the next
 * weeks as bars, a row each, weekends and holidays shaded. Nothing of the choice is stored. For groups and longer
 * periods its header links to the team vacation calendar, a page of its own.
 */
export function AvailabilityQuickViewDialog({
  onClose,
}: {
  onClose: () => void;
}) {
  const t = useTranslations();
  const [users, setUsers] = useState<EntityRef[]>([]);
  const [weeks, setWeeks] = useState<number>(DEFAULT_WEEKS);
  const me = useCurrentUserRef();
  const userIds = users.map((user) => user.id);
  const query = useQuery({
    queryKey: ["availabilityQuickView", userIds, weeks],
    queryFn: ({ signal }) => fetchUserAvailabilityView(userIds, weeks, signal),
    enabled: userIds.length > 0,
    // Adding or removing a person and switching the weeks keep the bars shown until the next ones are there.
    placeholderData: keepPreviousData,
  });
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent
        className="max-h-[90vh] overflow-y-auto sm:max-w-4xl"
        // Not onto the team calendar's button, the first in the dialog (its tooltip would open): the picker's search,
        // open at once, takes the focus.
        onOpenAutoFocus={(event) => event.preventDefault()}
      >
        <DialogHeader>
          {/* `pr-8`: the button keeps clear of the dialog's close button in the corner. A link in the body would
              be covered by the picker's popover, open as the dialog opens. */}
          <div className="flex items-center justify-between gap-2 pr-8">
            <DialogTitle>{t("vacation.quickView.title")}</DialogTitle>
            <HintTooltip text={t("vacation.quickView.teamCalendarHint")}>
              <Button asChild variant="outline" size="sm" className="gap-1.5">
                <GuardedLink href="/vacationExport" onClick={onClose}>
                  <HugeiconsIcon icon={UserGroupIcon} size={14} aria-hidden />
                  {t("vacation.quickView.teamCalendar")}
                </GuardedLink>
              </Button>
            </HintTooltip>
          </div>
          <DialogDescription>
            {t("vacation.quickView.selectHint")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <EntityMultiAutocomplete
            url={VACATION_EXPORT_USERS_URL}
            value={users}
            onChange={setUsers}
            selectMe={me}
            removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
            aria-label={t("vacation.quickView.user")}
            className="sm:flex-1"
            autoOpen
          />
          <div className="flex items-center gap-2 text-sm">
            <span className="text-muted-foreground">
              {t("vacation.quickView.weeks")}
            </span>
            <ToggleGroup
              type="single"
              variant="outline"
              size="sm"
              value={String(weeks)}
              onValueChange={(value) => value && setWeeks(Number(value))}
              aria-label={t("vacation.quickView.weeks")}
            >
              {WEEKS.map((w) => (
                <ToggleGroupItem key={w} value={String(w)}>
                  {w}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          </div>
        </div>
        {userIds.length > 0 &&
          (query.data ? (
            <AvailabilityQuickViewChart view={query.data} />
          ) : query.isError ? (
            <p className="text-sm text-destructive">{t("errorpage.title")}</p>
          ) : (
            <Skeleton className="h-28 w-full" />
          ))}
      </DialogContent>
    </Dialog>
  );
}
