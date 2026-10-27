"use client";

import { Kost2ReallocationList } from "@/components/features/kost2-reallocation/kost2-reallocation-list";

/**
 * The Kost2 reallocation runs (`/next/kost2-reallocation`, MenuItemDefId.KOST2_REALLOCATION). A concrete route, not a
 * REST list category; finance only, enforced by the endpoints behind it (`Kost2ReallocationRest`).
 */
export default function Kost2ReallocationPage() {
  return <Kost2ReallocationList />;
}
