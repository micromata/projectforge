import { BookEditPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/** deep links
// (e.g. /next/book/5) to the SPA shell, where the client reads the real id from the URL at runtime.
export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function BookEditPage() {
  return <BookEditPageClient />;
}
