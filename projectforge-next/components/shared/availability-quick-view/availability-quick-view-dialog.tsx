"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import {
  fetchUserAvailabilityView,
  VACATION_EXPORT_USERS_URL,
} from "@/lib/rs/vacation-export";
import { AvailabilityQuickViewChart } from "./availability-quick-view-chart";

/** The periods offered, in weeks from the beginning of this week; the backend shows 12 at most. */
const WEEKS = [4, 8, 12] as const;
const DEFAULT_WEEKS = 8;

/**
 * A quick look at whether a colleague is available: pick a user (only those the team vacation calendar would offer,
 * freelancers included) and see their vacations and availability entries of the next weeks as bars, weekends and
 * holidays shaded. Nothing of the choice is stored.
 */
export function AvailabilityQuickViewDialog({
  onClose,
}: {
  onClose: () => void;
}) {
  const t = useTranslations();
  const [user, setUser] = useState<EntityRef | null>(null);
  const [weeks, setWeeks] = useState<number>(DEFAULT_WEEKS);
  const userId = user?.id;
  const query = useQuery({
    queryKey: ["availabilityQuickView", userId, weeks],
    queryFn: ({ signal }) => fetchUserAvailabilityView(userId!, weeks, signal),
    enabled: userId != null,
    // Switching the weeks keeps the bars shown until the next ones are there.
    placeholderData: (previous, previousQuery) =>
      previousQuery?.queryKey[1] === userId
        ? keepPreviousData(previous)
        : undefined,
  });
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>{t("vacation.quickView.title")}</DialogTitle>
          <DialogDescription>
            {t("vacation.quickView.selectHint")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <EntityAutocomplete
            url={VACATION_EXPORT_USERS_URL}
            value={user}
            onChange={setUser}
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
        {userId != null &&
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
