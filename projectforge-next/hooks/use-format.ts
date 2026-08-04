"use client";

import { useMemo } from "react";
import { useAuth } from "./use-auth";
import { formatContextFrom, type FormatContext } from "@/lib/format";

/**
 * Locale, time zone and currency for formatting, taken from the logged-in user's
 * settings (userData). Falls back to the defaults in formatContextFrom while the
 * user status is still loading.
 */
export function useFormatContext(): FormatContext {
  const { user } = useAuth();
  return useMemo(() => formatContextFrom(user), [user]);
}
