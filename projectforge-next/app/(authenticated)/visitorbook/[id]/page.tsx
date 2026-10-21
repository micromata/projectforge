import { VisitorbookEditPageClient } from "./page-client";

export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function VisitorbookEditPage() {
  return <VisitorbookEditPageClient />;
}
