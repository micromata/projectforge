"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { DATA_TRANSFER_PAGE } from "@/components/features/datatransfer/datatransfer.page";

export default function DataTransferListPage() {
  return <EntityListPage page={DATA_TRANSFER_PAGE} />;
}
