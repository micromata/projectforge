import { DataTransferPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links
// (e.g. /next/datatransfer/5) to the SPA shell, where the client reads the real id from the URL at
// runtime. The placeholder is also the add route: /next/datatransfer/new is this very file.
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function DataTransferPage() {
  return <DataTransferPageClient />;
}
