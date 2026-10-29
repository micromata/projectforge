import { z } from "zod";
import type { EntityMetadata, FieldMetadata } from "@/lib/metadata/types";
import { fromMetadata } from "@/lib/validation/from-metadata";
import {
  BUSINESS_CARD_FIELDS,
  type BusinessCard,
  type BusinessCardField,
  type IncludedFields,
} from "./types";

const text = (i18nKey: string, maxLength = 255): FieldMetadata => ({
  dataType: "STRING",
  i18nKey,
  required: false,
  maxLength,
});

/**
 * The rules of BusinessCard (Kotlin), written by hand: it is no entity, so there is no generated metadata. The
 * name is the only mandatory field (BusinessCardService.save refuses a card without one).
 */
const FIELDS: Record<string, FieldMetadata> = {
  cardName: { ...text("businessCard.cardName"), required: true },
  ...Object.fromEntries(
    BUSINESS_CARD_FIELDS.map(({ field, labelKey }) => [
      field,
      text(labelKey, field === "comment" ? 1000 : 255),
    ])
  ),
};

export const BUSINESS_CARD_METADATA = {
  entity: "BusinessCard",
  historizable: false,
  fields: FIELDS,
} as const satisfies EntityMetadata;

const m = fromMetadata(BUSINESS_CARD_METADATA);

export const businessCardSchema = z.object({
  id: z.string().nullable(),
  addressId: z.number().nullable(),
  includePhoto: z.boolean(),
  cardName: m.requiredString("cardName"),
  ...Object.fromEntries(
    BUSINESS_CARD_FIELDS.map(({ field }) => [field, m.nullableString(field)])
  ),
});

export type BusinessCardValues = { [K in BusinessCardField]: string | null } & {
  id: string | null;
  addressId: number | null;
  includePhoto: boolean;
  cardName: string;
};

export const BUSINESS_CARD_FIELD_NAMES = [
  "cardName",
  ...BUSINESS_CARD_FIELDS.map(({ field }) => field),
] as const;

/** The card as the form holds it: a field Spring omitted becomes null. */
export function toFormValues(card: BusinessCard): BusinessCardValues {
  return {
    id: card.id ?? null,
    addressId: card.addressId ?? null,
    includePhoto: card.includePhoto === true,
    cardName: card.cardName ?? "",
    ...(Object.fromEntries(
      BUSINESS_CARD_FIELDS.map(({ field }) => [field, card[field] ?? null])
    ) as { [K in BusinessCardField]: string | null }),
  };
}

/**
 * Which fields are ticked at first. Taken over from an address: the business data that has a value, never the
 * private data (the user ticks it on purpose). A stored card: what it holds, it only holds what was ticked.
 */
export function initialIncluded(
  card: BusinessCard,
  fromAddress: boolean
): IncludedFields {
  return Object.fromEntries(
    BUSINESS_CARD_FIELDS.map(({ field, private: isPrivate }) => [
      field,
      !!card[field]?.trim() && !(fromAddress && isPrivate),
    ])
  ) as IncludedFields;
}

/** The card as it is saved and previewed: the unticked fields are left out. */
export function postedCard(
  values: BusinessCardValues,
  included: IncludedFields
): BusinessCard {
  const result: BusinessCard = { ...values };
  for (const { field } of BUSINESS_CARD_FIELDS) {
    if (!included[field]) result[field] = null;
  }
  return result;
}
