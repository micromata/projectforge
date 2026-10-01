"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { EntityOrTextField } from "@/components/shared/form/entity-or-text-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchOne } from "@/lib/rs/client";
import { cn } from "@/lib/utils";

/** The account of a customer, as `/rs/customer/{id}` answers it — `Customer.konto`. */
interface CustomerDetail {
  konto?: { id?: number | null } | null;
}

/**
 * The address block of an account, as `/rs/account/{id}` answers it (`KontoPagesRest.transformFromDB`,
 * which copies the whole `KontoDO`).
 *
 * Read separately, and this is not a detour that can be shortened: `Customer.copyFrom` builds its account
 * with `Konto(KontoDO)`, whose constructor is `copyFromMinimal` — so the customer's own answer carries
 * nothing but the account's id and display name, however many fields the `Konto` DTO declares.
 */
interface AccountDetail {
  contactPerson?: string | null;
  street?: string | null;
  zipCode?: string | null;
  city?: string | null;
  country?: string | null;
  vatId?: string | null;
  leitwegId?: string | null;
  eInvoiceEmail?: string | null;
}

/**
 * The account's field on the left, the invoice's field on the right — the whole address block of the
 * e-invoice, in the order the `customer` section shows it.
 */
const ADDRESS_FIELDS: [keyof AccountDetail, string][] = [
  ["contactPerson", "customerContactPerson"],
  ["street", "customerAddress"],
  ["zipCode", "customerZipCode"],
  ["city", "customerCity"],
  ["country", "customerCountry"],
  ["vatId", "customerVatId"],
  ["leitwegId", "customerLeitwegId"],
  ["eInvoiceEmail", "customerEInvoiceEmail"],
];

/**
 * The project and the customer of an invoice — the customer either picked from the list or typed as free text
 * (see EntityOrTextField) — fields that only make sense together.
 *
 * Custom rather than declared, twice over: `customer` and `project` reference `KundeDO`/`ProjektDO`, for
 * which there is no `UIDataType`, so the generated metadata cannot carry them however the entity is
 * annotated (hence `metadataLess`); and picking one of them fills in what it knows — the project its
 * customer, the customer its billing address.
 *
 * The autofill only ever fills what is **empty**: an invoice may deliberately name a different customer
 * than its project does (`fibu.rechnung.hint.kannVonProjektKundenAbweichen`) or a billing address that
 * differs from the one on file, and overwriting that would quietly undo the user's entry. The free-text
 * customer blocks the customer being filled in for the same reason — it is what someone typed because no
 * customer record fits.
 */
export function CustomerProjectFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();

  async function fillFromProject(project: EntityRef | null) {
    if (!project) return;
    // Read after the pick rather than from the autosearch result: `{entity}/autosearch` answers
    // `DisplayObject`s (id and display name only), so the customer has to be fetched.
    const detail = await fetchOne<{ customer?: EntityRef | null }>(
      "project",
      project.id
    );
    if (form.getFieldValue("kundeText")) return;
    if (!detail.customer || form.getFieldValue("customer")) return;
    form.setFieldValue("customer", detail.customer);
    // Chained on purpose: a customer the project brought along is as much a picked customer as one
    // chosen by hand, and its address is what the e-invoice needs either way.
    await fillFromCustomer(detail.customer);
  }

  /**
   * The address block from the customer's account — what `EInvoiceService` needs to produce an XRechnung
   * and what nobody should have to copy by hand.
   *
   * Two reads, because the customer only names its account (see [AccountDetail]). A customer without one
   * fills nothing: the address lives on the account, not on the customer.
   */
  async function fillFromCustomer(customer: EntityRef | null) {
    if (!customer) return;
    const { konto } = await fetchOne<CustomerDetail>("customer", customer.id);
    if (konto?.id == null) return;
    const account = await fetchOne<AccountDetail>("account", konto.id);
    for (const [from, to] of ADDRESS_FIELDS) {
      const value = account[from];
      if (value && !form.getFieldValue(to)) form.setFieldValue(to, value);
    }
  }

  return (
    // Stacked, with the row gap of the section's grid: the two fields are the top two rows of the
    // invoice's customer column (the head section's `aside`), one above the other like the rows below.
    <div className={cn("grid grid-cols-1 gap-y-4", className)}>
      <EntityAutocompleteField
        name="project"
        label={t("fibu.projekt._")}
        entity="project"
        metadataLess
        onPicked={(project) => void fillFromProject(project)}
      />
      <EntityOrTextField
        entityName="customer"
        textName="kundeText"
        label={t("fibu.kunde._")}
        entity="customer"
        metadataLess
        onPicked={(customer) => void fillFromCustomer(customer)}
        // Says what the free text is for: a customer that has no record of its own, which may differ
        // from the project's. Picking one clears the other, and the backend drops the free text beside a
        // customer as well (`OutgoingInvoiceEntityRest.transformForDB`), so the two cannot disagree.
        hint={t("fibu.rechnung.hint.kannVonProjektKundenAbweichen")}
      />
    </div>
  );
}
