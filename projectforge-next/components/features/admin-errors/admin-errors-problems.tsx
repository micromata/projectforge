"use client";

import { useTranslations } from "next-intl";
import type {
  LogGroupEntry,
  LogGroupFilter,
  LogGroupList,
} from "@/lib/rs/admin-errors";
import { AdminErrorsFilters } from "./admin-errors-filters";
import { AdminErrorsTable } from "./admin-errors-table";

/** The problems tab of the problem dashboard: the server's filter and the table of the problems. */
export function AdminErrorsProblems({
  data,
  isFetching,
  filter,
  subsystemTitle,
  onFilterChange,
  onRemoveSubsystem,
  search,
  onSearchChange,
  onOpen,
}: {
  data: LogGroupList;
  isFetching: boolean;
  filter: LogGroupFilter;
  subsystemTitle: string | null;
  onFilterChange: (filter: LogGroupFilter) => void;
  onRemoveSubsystem: () => void;
  search: string;
  onSearchChange: (search: string) => void;
  onOpen: (entry: LogGroupEntry) => void;
}) {
  const t = useTranslations();
  return (
    <>
      <AdminErrorsFilters
        filter={filter}
        subsystemTitle={subsystemTitle}
        onChange={onFilterChange}
        onRemoveSubsystem={onRemoveSubsystem}
      />
      {data.total > data.entries.length && (
        <p className="text-muted-foreground">
          {t("system.admin.adminErrors.more", {
            arg0: data.entries.length,
            arg1: data.total,
          })}
        </p>
      )}
      {data.entries.length === 0 ? (
        <p className="py-4 text-muted-foreground">
          {t("system.admin.adminErrors.none")}
        </p>
      ) : (
        <AdminErrorsTable
          entries={data.entries}
          isFetching={isFetching}
          search={search}
          onSearchChange={onSearchChange}
          onOpen={onOpen}
        />
      )}
    </>
  );
}
