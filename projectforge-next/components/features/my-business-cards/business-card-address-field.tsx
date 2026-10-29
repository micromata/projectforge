"use client";

import { useTranslations } from "next-intl";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The address the data of the card is taken over from; the suggested one (most likely the own) says so. In the
 * shell of the other fields, though it is no value of the card (the card keeps only its id, set on pick).
 */
export function BusinessCardAddressField({
  value,
  suggestionAddressId,
  onPick,
}: {
  value: EntityRef | null;
  suggestionAddressId: number | null;
  onPick: (ref: EntityRef | null) => void;
}) {
  const t = useTranslations();
  const ids = useFieldIds();
  return (
    <FieldShell
      label={t(leafKeyOf("businessCard.address", t.has))}
      hint={t("businessCard.address.tooltip")}
      invalid={false}
      errors={[]}
      ids={ids}
    >
      <EntityAutocomplete
        id={ids.controlId}
        url="address/autosearch?search=:search"
        value={value}
        onChange={onPick}
      />
      {value && value.id === suggestionAddressId && (
        <p className="text-xs text-muted-foreground">
          {t("businessCard.address.suggestion")}
        </p>
      )}
    </FieldShell>
  );
}
