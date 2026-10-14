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
import {
  fetchVacationAccount,
  recalculateRemainingLeave,
} from "@/lib/rs/vacation-account";
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
 */
export function VacationAccountPage() {
  const t = useTranslations();
  const params = useSearchParams();
  const [employeeId, setEmployeeId] = useState<number | undefined>(
    () => Number(params.get("employeeId")) || undefined
  );
  const queryClient = useQueryClient();
  const queryKey = ["vacationAccount", employeeId];

  const query = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchVacationAccount(employeeId, signal),
    // The tables stay while another employee's account loads instead of collapsing to "loading".
    placeholderData: keepPreviousData,
  });
  const account = query.data;
  const employee = account?.employee;

  const recalculate = useMutation({
    mutationFn: () => recalculateRemainingLeave(employee!.id),
    onSuccess: (fresh) => queryClient.setQueryData(queryKey, fresh),
  });

  function selectEmployee(id: number) {
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
          recalculating={recalculate.isPending}
          onRecalculate={() => recalculate.mutate()}
        />
      </PageTitleRow>
      <div className="flex flex-col gap-4 px-4 pb-6">
        {query.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {query.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
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
