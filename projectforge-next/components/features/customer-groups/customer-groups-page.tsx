"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { fetchCustomerGroups } from "@/lib/rs/customer-groups";
import { CustomerGroupsForm } from "./customer-groups-form";

/**
 * The customer groups page (`/next/customerGroups`): which customers form a group (ACME Germany, ACME
 * Logistics → ACME) and which groups and customers form a business unit, as offered by the customer
 * and business-unit checklists of the order book, the invoices, the projects and cost 2.
 *
 * PF_Finance and PF_Controlling only; the endpoint checks it and answers everybody else with 403.
 */
export function CustomerGroupsPage() {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ["customerGroups"],
    queryFn: ({ signal }) => fetchCustomerGroups(signal),
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.fibu._")}
        title={t("fibu.customerGroups.title")}
      />
      {query.isPending && (
        <p className="px-4 text-sm text-muted-foreground">{t("loading")}</p>
      )}
      {query.isError && (
        <p className="px-4 text-sm text-destructive">
          {t("access.exception.noAccess")}
        </p>
      )}
      {query.data && <CustomerGroupsForm data={query.data} />}
    </PageShell>
  );
}
