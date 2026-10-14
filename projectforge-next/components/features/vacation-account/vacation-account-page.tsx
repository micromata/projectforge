"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
import { LeaveAccountEntriesTable } from "./leave-account-entries-table";
import { VacationAccountActions } from "./vacation-account-actions";
import { VacationEntriesTable } from "./vacation-entries-table";
import { VacationAccountSummary } from "./vacation-account-summary";

/**
 * The vacation account ("Urlaubskonto"), a hand-built standalone page: the leave statistics of the previous,
 * current and next year, the vacations of these years and the corrections of the leave account.
 *
 * HR staff pick the employee (`?employeeId=`, kept in the url); without one the backend shows the one shown last,
 * at first the own one. Everybody else always sees their own account.
 *
 * Asked anew on every visit: a vacation saved or deleted from here changes the numbers (the vacation form
 * invalidates the account as well, see VACATION_PAGE.extraInvalidateKeys).
 */
export function VacationAccountPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [employeeId, setEmployeeId] = useState<number | undefined>(
    () => Number(params.get("employeeId")) || undefined
  );
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

  const recalculate = useMutation({
    mutationFn: (id: number) => recalculateRemainingLeave(id),
    // Asked anew rather than stored under the shown key: the employee may have been switched meanwhile.
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: VACATION_ACCOUNT_QUERY_KEY }),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  function selectEmployee(id: number) {
    // The account without a parameter is the one shown last (the backend stores it): the way back from a
    // vacation (`?returnTo=/vacationAccount`) has none, and must not find the one shown before in the cache.
    queryClient.removeQueries({
      queryKey: [...VACATION_ACCOUNT_QUERY_KEY, undefined],
      exact: true,
    });
    setEmployeeId(id);
    updateSearchParams({ employeeId: String(id) }, "replace");
  }

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.vacation._")}
        title={t("vacation.leaveaccount.title")}
      >
        <VacationAccountActions
          account={account}
          // Not while another employee's account loads: the button would recalculate the one still shown.
          recalculating={recalculate.isPending || query.isPlaceholderData}
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
            {account.leaveAccountEntries.length > 0 && (
              <LeaveAccountEntriesTable account={account} />
            )}
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
