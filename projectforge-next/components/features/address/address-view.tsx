"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { notFound } from "next/navigation";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { useReadAccessGuard } from "@/hooks/use-read-access-guard";
import { RsError } from "@/lib/rs/client";
import { ADDRESS_ROUTE } from "./address-routes";
import { AddressViewActions } from "./address-view-actions";
import { AddressViewDetails } from "./address-view-details";
import { useAddressHeading } from "./use-address-heading";
import { useAddressView } from "./use-address-view";

/**
 * The pages that open an address for reading and expect the user back (`?returnTo=`): the calendar (a birthday),
 * the phone call page (the address of the number). A whitelist, so no url from outside is followed.
 */
const RETURN_TARGETS: Record<string, string> = {
  "/calendar": "menu.calendar",
  "/phoneCall": "menu.phoneCall",
};

/**
 * The view of an address as a page — what a search hit, a birthday in the calendar and the phone call page open:
 * the image, the phone numbers to call, the contact data and the addresses, printable. Successor of the React view
 * (`AddressViewPageRest`); the form is a sub route (`/address/{id}/edit`). A row of the list shows the same view in
 * a dialog instead (AddressViewDialog), as the React list did.
 */
export function AddressView({ id }: { id: number }) {
  const t = useTranslations();
  const { query, refresh } = useAddressView(id);
  const access = useReadAccessGuard("address", query.error);
  const returnTo = useSearchParams().get("returnTo");
  const back =
    returnTo && RETURN_TARGETS[returnTo]
      ? { route: returnTo, labelKey: RETURN_TARGETS[returnTo] }
      : { route: ADDRESS_ROUTE, labelKey: "address.title.list" };
  const view = query.data;
  const heading = useAddressHeading(view?.address);

  if (access.denied) return null;
  if (query.error instanceof RsError && query.error.status === 404) notFound();

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.common")}
        title={heading || t("address.view.title")}
      >
        {view && <AddressViewActions address={view.address} back={back} />}
      </PageTitleRow>
      {!view ? (
        <div className="flex justify-center py-8">
          <Spinner className="h-6 w-6 border-2" />
        </div>
      ) : (
        <AddressViewDetails view={view} onFavoriteChanged={refresh} />
      )}
    </PageShell>
  );
}
