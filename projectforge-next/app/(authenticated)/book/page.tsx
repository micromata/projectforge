"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { BOOK_PAGE } from "@/components/features/book/book.page";

export default function BookListPage() {
  return <EntityListPage page={BOOK_PAGE} />;
}
