"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchUserStatus } from "@/lib/rs/client";
import type { UserStatus } from "@/lib/rs/types";
import { useLocale } from "@/i18n/locale-provider";
import { normalizeLocale } from "@/i18n/config";

const EMPTY: string[] = [];

export function useAuth() {
  const query = useQuery<UserStatus>({
    queryKey: ["userStatus"],
    queryFn: ({ signal }) => fetchUserStatus(signal),
    retry: false,
    // A system alert message an admin sets now (a downtime announced for 13:00) has to reach a tab
    // that is already open, so this query refetches when the tab regains focus and when a page
    // change finds it stale - the global default is refetchOnWindowFocus: false (see QueryProvider).
    // Deliberately no refetchInterval: every call touches the HTTP session, so a timer would keep
    // an idle tab logged in forever.
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
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
    /** Member of the admin group — gates menu entries this app declares itself. */
    isAdmin: !!query.data?.adminUser,
    /** Member of HR, finance, orga or admin: may show inactive users/employees in pickers (useShowInactive). */
    mayShowInactivePersons: !!query.data?.showInactivePersons,
    /** JIRA config for client-side issue linking, null where JIRA is not configured (see JiraConfig). */
    jira: query.data?.jira ?? null,
    /** The summary of the notifications visible in the app, null before the status is loaded. */
    notifications: query.data?.notifications ?? null,
    /** The ids of the switched off features, see useFeatureEnabled. */
    disabledFeatures: query.data?.disabledFeatures ?? EMPTY,
    /** The next routes of the switched off features, see FeatureGuard. */
    featureRoutes: query.data?.featureRoutes ?? null,
    error: query.error,
    refetch: query.refetch,
  };
}
