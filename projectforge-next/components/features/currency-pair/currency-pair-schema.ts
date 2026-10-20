import { z } from "zod";
import { CURRENCY_PAIR_METADATA } from "@/lib/metadata/currency-pair.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

const m = fromMetadata(CURRENCY_PAIR_METADATA);

export const currencyPairSchema = z.object({
  // null while the pair is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  // Stored in upper case (CurrencyPairEntityRest.transformForDB); that both differ is the backend's check.
  sourceCurrency: m.requiredString("sourceCurrency"),
  targetCurrency: m.requiredString("targetCurrency"),
  comment: m.nullableString("comment"),
});

export type CurrencyPairValues = z.infer<typeof currencyPairSchema>;

export const CURRENCY_PAIR_FIELDS = Object.keys(
  currencyPairSchema.shape
) as readonly (keyof CurrencyPairValues)[];
