"use client";

import { useTranslations } from "next-intl";
import { BOOK_METADATA } from "@/lib/metadata/book.generated";
import {
  fromMetadata,
  type SelectOption,
} from "@/lib/validation/from-metadata";

const m = fromMetadata(BOOK_METADATA);

/**
 * The options of the two enum selects: values and labels both come from the generated metadata, which
 * read them off BookType / BookStatus including each constant's `i18nKey` (`I18nEnum.i18nKey`). The
 * key is no longer rebuilt from the constant's name here — that guess ("book.type." plus the lower
 * case name without underscores) happened to hold for these two enums and for no others.
 *
 * `useTranslations()` without a namespace, since the keys are absolute (`book.type.audiobook`,
 * `book.status.present`).
 */
export function useBookTypeOptions(): SelectOption[] {
  const t = useTranslations();
  return m.enumOptions("type", t);
}

export function useBookStatusOptions(): SelectOption[] {
  const t = useTranslations();
  return m.enumOptions("status", t);
}
