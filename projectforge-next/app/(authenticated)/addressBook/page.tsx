"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ADDRESS_BOOK_PAGE } from "@/components/features/addressBook/addressBook.page";

export default function AddressBookListPage() {
  return <EntityListPage page={ADDRESS_BOOK_PAGE} />;
}
