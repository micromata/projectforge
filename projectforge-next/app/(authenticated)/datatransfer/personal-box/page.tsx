"use client";

import { DataTransferPersonalBoxPage } from "@/components/features/datatransfer/datatransfer-personal-box-page";

// A static sibling of [id], so /next/datatransfer/personal-box is served as a file of its own.
export default function DataTransferPersonalBoxRoute() {
  return <DataTransferPersonalBoxPage />;
}
