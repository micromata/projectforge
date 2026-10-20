"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { CURRENCY_PAIR_PAGE } from "@/components/features/currency-pair/currency-pair.page";

export default function CurrencyPairListPage() {
  return <EntityListPage page={CURRENCY_PAIR_PAGE} />;
}
