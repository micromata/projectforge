import { AppLoginPathPageClient } from "./page-client";

// Static export emits a single placeholder route; Spring forwards /next/appLogin/<login> to this shell, where the
// client reads the packed parameters from the URL at runtime.
export function generateStaticParams() {
  return [{ login: "new" }];
}

export default function AppLoginPathRoute() {
  return <AppLoginPathPageClient />;
}
