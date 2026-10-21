"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { MEMO_PAGE } from "@/components/features/memo/memo.page";

export default function MemoListPage() {
  return <EntityListPage page={MEMO_PAGE} />;
}
