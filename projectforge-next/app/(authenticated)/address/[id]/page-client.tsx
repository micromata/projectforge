"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { ADDRESS_PAGE } from "@/components/features/address/address.page";
import { AddressView } from "@/components/features/address/address-view";
import { useAddressEditOutcome } from "@/components/features/address/use-address-edit-outcome";

/**
 * `/address/{id}` is the view of a stored address — what a row of the list opens —, `/address/new` the form of a
 * new one: one dynamic segment serves both under the static export. The form of a stored address is
 * `/address/{id}/edit`.
 */
export function AddressPageClient() {
  const outcome = useAddressEditOutcome(null);
  const raw = useRouteParams<{ id: string }>("/address/[id]")?.id;
  if (raw === undefined) return null;
  if (raw === "new") {
    return (
      <PageShell>
        <EntityEditPage page={ADDRESS_PAGE} id={null} outcome={outcome} />
      </PageShell>
    );
  }
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return <AddressView id={id} />;
}
