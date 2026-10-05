"use client";

import { useTranslations } from "next-intl";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { SearchInput } from "@/components/shared/list/search-input";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import type {
  LogCategory,
  LogGroupFilter,
  LogGroupStatusFilter,
} from "@/lib/rs/admin-errors";
import {
  CATEGORIES,
  CATEGORY_KEYS,
  STATUS_FILTER_KEYS,
} from "./admin-errors-labels";

const ALL_CATEGORIES = "ALL";

const DAYS = [1, 7, 30, 0];

/** Search, status, category and period of the error dashboard's list. */
export function AdminErrorsFilters({
  filter,
  onChange,
}: {
  filter: LogGroupFilter;
  onChange: (filter: LogGroupFilter) => void;
}) {
  const t = useTranslations();
  const days = (value: number) =>
    value === 0
      ? t("system.admin.adminErrors.days.all")
      : value === 1
        ? t("system.admin.adminErrors.days.1")
        : t("system.admin.adminErrors.days.n", { arg0: value });
  return (
    <div className="flex flex-wrap items-center gap-4">
      <div className="relative w-full max-w-md">
        <SearchInput
          value={filter.search ?? ""}
          onChange={(search) => onChange({ ...filter, search })}
        />
      </div>
      <div className="flex items-center gap-2">
        <Label htmlFor="admin-errors-status">{t("status")}</Label>
        <Select
          value={filter.status}
          onValueChange={(status) =>
            onChange({ ...filter, status: status as LogGroupStatusFilter })
          }
        >
          <SelectTrigger id="admin-errors-status" className="h-9 w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {(Object.keys(STATUS_FILTER_KEYS) as LogGroupStatusFilter[]).map(
              (status) => (
                <SelectItem key={status} value={status}>
                  {t(STATUS_FILTER_KEYS[status])}
                </SelectItem>
              )
            )}
          </SelectContent>
        </Select>
      </div>
      <div className="flex items-center gap-2">
        <Label htmlFor="admin-errors-category">
          {t("system.admin.adminErrors.category")}
        </Label>
        <Select
          value={filter.category ?? ALL_CATEGORIES}
          onValueChange={(category) =>
            onChange({
              ...filter,
              category:
                category === ALL_CATEGORIES ? null : (category as LogCategory),
            })
          }
        >
          <SelectTrigger id="admin-errors-category" className="h-9 w-56">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_CATEGORIES}>
              {t("system.admin.adminErrors.category.all")}
            </SelectItem>
            {CATEGORIES.map((category) => (
              <SelectItem key={category} value={category}>
                {t(CATEGORY_KEYS[category])}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex items-center gap-2">
        <Label htmlFor="admin-errors-days">
          {t("system.admin.adminErrors.days")}
        </Label>
        <Select
          value={String(filter.days ?? 0)}
          onValueChange={(value) =>
            onChange({ ...filter, days: Number(value) })
          }
        >
          <SelectTrigger id="admin-errors-days" className="h-9 w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {DAYS.map((value) => (
              <SelectItem key={value} value={String(value)}>
                {days(value)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
