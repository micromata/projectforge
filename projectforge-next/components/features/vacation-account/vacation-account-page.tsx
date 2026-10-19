"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { RsError } from "@/lib/rs/client";
import {
  fetchVacationAccount,
  recalculateRemainingLeave,
  VACATION_ACCOUNT_QUERY_KEY,
} from "@/lib/rs/vacation-account";
import { toast } from "@/lib/toast";
import { updateSearchParams } from "@/lib/search-params";
import { AvailabilityQuickViewButton } from "@/components/shared/availability-quick-view/availability-quick-view-button";
import { AvailabilityEntriesTable } from "./availability-entries-table";
import { LeaveAccountEntriesTable } from "./leave-account-entries-table";
import { VacationAccountActions } from "./vacation-account-actions";
import { VacationEntriesTable } from "./vacation-entries-table";
import { VacationAccountSummary } from "./vacation-account-summary";

/**
 * The vacation account ("Urlaubskonto"), a hand-built standalone page: the leave statistics of the previous,
 * current and next year, the vacations of these years, the corrections of the leave account and, apart from the
 * vacations, the availability entries of these years.
 *
 * HR staff pick the employee (`?employeeId=`, kept in the url); without one the backend shows the one shown last,
 * at first the own one. Everybody else always sees their own account.
 *
 * Asked anew on every visit: a vacation saved or deleted from here changes the numbers (the vacation form
 * invalidates the account as well, see VACATION_PAGE.extraInvalidateKeys, as does the availability form).
 */
export function VacationAccountPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [employeeId, setEmployeeId] = useState<number | undefined>(
    () => Number(params.get("employeeId")) || undefined
  );
  // The employee just picked, shown in the picker until their account arrives (the one still shown is the
  // previous employee's).
  const [picked, setPicked] = useState<EntityRef | null>(null);
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: [...VACATION_ACCOUNT_QUERY_KEY, employeeId],
    queryFn: ({ signal }) => fetchVacationAccount(employeeId, signal),
    // The tables stay while another employee's account loads instead of collapsing to "loading".
    placeholderData: keepPreviousData,
    staleTime: 0,
  });
  const account = query.data;
  const employee = account?.employee;
  const switching = query.isPlaceholderData;

  // The account without a parameter is the one shown last (the backend stores it, for a picked employee and
  // for one given by `?employeeId=` alike): the way back from a vacation (`?returnTo=/vacationAccount`) has
  // none, and must not find the one shown before in the cache.
  useEffect(() => {
    if (employeeId == null) return;
    queryClient.removeQueries({
      queryKey: [...VACATION_ACCOUNT_QUERY_KEY, undefined],
      exact: true,
    });
  }, [employeeId, queryClient]);

  const recalculate = useMutation({
    mutationFn: (id: number) => recalculateRemainingLeave(id),
    // Asked anew rather than stored under the shown key: the employee may have been switched meanwhile.
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: VACATION_ACCOUNT_QUERY_KEY }),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  function selectEmployee(ref: EntityRef) {
    setPicked(ref);
    setEmployeeId(ref.id);
    updateSearchParams({ employeeId: String(ref.id) }, "replace");
  }

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.vacation._")}
        title={t("vacation.leaveaccount.title")}
      >
        <AvailabilityQuickViewButton size="sm" />
        <VacationAccountActions
          account={account}
          // Not while another employee's account loads: the buttons would act on the one still shown.
          switching={switching}
          recalculating={recalculate.isPending}
          onRecalculate={() => employee && recalculate.mutate(employee.id)}
        />
      </PageTitleRow>
      <div className="flex flex-col gap-4 px-4 pb-6">
        {query.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {query.isError && (
          <p className="text-sm text-destructive">
            {query.error instanceof RsError && query.error.status === 403
              ? t("access.exception.noAccess")
              : t("errorpage.title")}
          </p>
        )}
        {account && (
          <VacationAccountSummary
            account={account}
            selected={switching ? picked : undefined}
            onSelectEmployee={selectEmployee}
          />
        )}
        {account && !employee && (
          <p className="text-sm text-muted-foreground">
            {t("access.exception.noEmployeeToUser")}
          </p>
        )}
        {account && employee && (
          <>
            <VacationEntriesTable account={account} />
            {(account.leaveAccountEntries.length > 0 ||
              account.newLeaveAccountEntryUrl) && (
              <LeaveAccountEntriesTable
                account={account}
                switching={switching}
              />
            )}
            <AvailabilityEntriesTable account={account} switching={switching} />
          </>
        )}
        <Alert>
          <AlertTitle>{t("vacation.subscription._")}</AlertTitle>
          <AlertDescription>{t("vacation.subscription.info")}</AlertDescription>
        </Alert>
      </div>
    </PageShell>
  );
}
