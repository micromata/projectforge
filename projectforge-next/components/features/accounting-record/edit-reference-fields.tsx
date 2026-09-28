"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The cost1/cost2/konto/gegenKonto pickers of an accounting record. Custom because their entities either have
 * no `UIDataType` (KontoDO — hence `metadataLess`) or are searched under a REST category that differs from the
 * field name (kost1 → `cost1`, kost2 → `cost2`, konto/gegenKonto → `account`).
 *
 * All four are `disabled`: a record is DATEV-imported and its bookings are not editable — only its `comment`
 * is (the Wicket `AccountingRecordEditForm` sets every field but the comment read-only). They stay visible so
 * the record reads in full, but they cannot be repointed.
 */

export function Kost1Field({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="kost1"
      label={t(leafKeyOf("fibu.kost1", t.has))}
      entity="cost1"
      disabled
      className={className}
    />
  );
}

export function Kost2Field({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="kost2"
      label={t(leafKeyOf("fibu.kost2", t.has))}
      entity="cost2"
      disabled
      className={className}
    />
  );
}

export function KontoField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="konto"
      label={t(leafKeyOf("fibu.buchungssatz.konto", t.has))}
      entity="account"
      metadataLess
      disabled
      className={className}
    />
  );
}

export function GegenKontoField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="gegenKonto"
      label={t(leafKeyOf("fibu.buchungssatz.gegenKonto", t.has))}
      entity="account"
      metadataLess
      disabled
      className={className}
    />
  );
}
