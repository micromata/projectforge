"use client";

import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { Checkbox } from "@/components/ui/checkbox";
import {
  BUSINESS_CARD_FIELDS,
  type BusinessCard,
  type BusinessCardField,
  type IncludedFields,
} from "./types";

/**
 * The fields of one group (business or private) of a card, each with the box that puts it on the card. An
 * unticked field is locked (what is typed there would not be saved). Ticking an empty field fills it from the
 * source address again, so a field left out once is not lost for good.
 */
export function BusinessCardFields({
  privateFields,
  included,
  onIncluded,
  source,
}: {
  privateFields: boolean;
  included: IncludedFields;
  onIncluded: (field: BusinessCardField, value: boolean) => void;
  /** The data of the source address, if the card has one (see BusinessCardRest.fromAddress). */
  source: BusinessCard | undefined;
}) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const tick = (field: BusinessCardField, value: boolean) => {
    if (value && !form.getFieldValue(field)?.trim() && source?.[field]) {
      form.setFieldValue(field, source[field]);
    }
    onIncluded(field, value);
  };
  return (
    <div className="grid gap-x-4 gap-y-3 sm:grid-cols-2">
      {BUSINESS_CARD_FIELDS.filter((f) => f.private === privateFields).map(
        ({ field, labelKey }) => (
          <div key={field} className="flex items-end gap-2">
            <Checkbox
              className="mb-2.5"
              checked={included[field]}
              onCheckedChange={(checked) => tick(field, checked === true)}
              aria-label={t(labelKey)}
            />
            <InputField
              className="flex-1"
              name={field}
              label={t(labelKey)}
              disabled={!included[field]}
            />
          </div>
        )
      )}
    </div>
  );
}
