import { DataTransferPublicPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/public/datatransfer/<token>
// to this shell, where the client reads the real token from the URL at runtime.
export function generateStaticParams() {
  return [{ token: "new" }];
}

export default function DataTransferPublicRoute() {
  return <DataTransferPublicPageClient />;
}
