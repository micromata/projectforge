"use client";

import { BirthdayButlerPage } from "@/components/features/birthday-butler/birthday-butler-page";

/**
 * The birthday butler (`/next/birthdayButler`), successor of the dynamic React page
 * `react/birthdayButler/dynamic`. Takes no parameters, so no `<Suspense>`/`useSearchParams` is needed.
 */
export default function BirthdayButlerRoute() {
  return <BirthdayButlerPage />;
}
