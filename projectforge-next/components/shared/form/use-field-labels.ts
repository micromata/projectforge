"use client";

import { useTranslations } from "next-intl";
import type { EntityMetadata } from "@/lib/metadata/types";
import { labelKeyFor } from "@/lib/page-def/define-page";

/**
 * The label of a field, as the declared sections derive it: the `i18nKey` of the entity's
 * `@PropertyInfo`, with the generator's `._` leaf where a key is both a text and a namespace.
 *
 * For hand-written rows — an order position, an instalment of a payment schedule — which cannot go
 * through [DeclaredSection] but must be labelled the same way. Without this each row would spell its
 * keys out and the wording would drift from the entity's, which is exactly what the declaration exists
 * to prevent.
 */
export function useFieldLabels<M extends EntityMetadata>(metadata: M) {
  const t = useTranslations();
  const translate = t as unknown as ((key: string) => string) & {
    has: (key: string) => boolean;
  };
  return (name: keyof M["fields"] & string, override?: string) =>
    translate(labelKeyFor(metadata, name, translate.has, override));
}
