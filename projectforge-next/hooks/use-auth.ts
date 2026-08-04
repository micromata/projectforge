"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchUserStatus } from "@/lib/rs/client";
import type { UserStatus } from "@/lib/rs/types";
import { useLocale } from "@/i18n/locale-provider";
import { normalizeLocale } from "@/i18n/config";

export function useAuth() {
  const query = useQuery<UserStatus>({
    queryKey: ["userStatus"],
    queryFn: ({ signal }) => fetchUserStatus(signal),
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  // The user's backend locale wins over cookie/browser detection.
  const { locale, setLocale } = useLocale();
  const backendLocale = normalizeLocale(query.data?.userData?.locale);
  useEffect(() => {
    if (backendLocale && backendLocale !== locale) setLocale(backendLocale);
  }, [backendLocale, locale, setLocale]);

  return {
    user: query.data?.userData ?? null,
    systemData: query.data?.systemData ?? null,
    alertMessage: query.data?.alertMessage,
    isLoading: query.isLoading,
    isAuthenticated: !!query.data?.userData,
    error: query.error,
    refetch: query.refetch,
  };
}
