"use client";

import { useTranslations } from "next-intl";
import type { SelectOption } from "./book-edit-fields";
import {
  BOOK_STATUS_VALUES,
  BOOK_TYPE_VALUES,
  type BookStatus,
  type BookType,
} from "../types";

/**
 * The i18n key of an enum value, as the backend builds it (`BookType.i18nKey` = "book.type." plus a
 * lower case key without underscores).
 */
function i18nKey(value: string): string {
  return value.toLowerCase().replace(/_/g, "");
}

export function useBookTypeOptions(): SelectOption[] {
  const t = useTranslations("book.type");
  return BOOK_TYPE_VALUES.map((value: BookType) => ({
    value,
    label: t(i18nKey(value)),
  }));
}

export function useBookStatusOptions(): SelectOption[] {
  const t = useTranslations("book.status");
  return BOOK_STATUS_VALUES.map((value: BookStatus) => ({
    value,
    label: t(i18nKey(value)),
  }));
}
