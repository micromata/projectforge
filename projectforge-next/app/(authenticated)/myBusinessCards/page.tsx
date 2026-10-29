"use client";

import { MyBusinessCardsPage } from "@/components/features/my-business-cards/my-business-cards-page";

/**
 * The digital business cards of the logged-in user (`/next/myBusinessCards`, BusinessCardRest). Reached from the
 * user menu and the address list, not from the main menu.
 */
export default function MyBusinessCardsRoute() {
  return <MyBusinessCardsPage />;
}
