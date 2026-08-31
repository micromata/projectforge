"use client";

import { useEffect, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTheme } from "next-themes";
import { useAuth } from "@/hooks/use-auth";
import {
  fetchThemeSetting,
  saveThemeSetting,
  type ThemePreference,
} from "@/lib/rs/ui-settings";

const THEME_QUERY_KEY = ["uiTheme"] as const;

/**
 * Hydrates `next-themes` from the user's server-stored theme so the choice follows them across devices.
 * Mount once in the authenticated app shell (TopNavigation).
 *
 * Applied only once per mount: `next-themes` already painted the last local choice from localStorage before
 * this GET returns, so a returning device never flashes; a device whose localStorage disagrees with the server
 * shows one brief transition once the value arrives. The once-guard is what keeps a later local change (which
 * updates `theme` but not the server query) from being reverted back to the fetched value.
 */
export function useThemeSync(): void {
  const { isAuthenticated } = useAuth();
  const { setTheme } = useTheme();
  const applied = useRef(false);

  const { data } = useQuery({
    queryKey: THEME_QUERY_KEY,
    queryFn: ({ signal }) => fetchThemeSetting(signal),
    enabled: isAuthenticated,
    staleTime: Infinity,
  });

  useEffect(() => {
    const serverTheme = data?.theme;
    if (!applied.current && serverTheme) {
      applied.current = true;
      setTheme(serverTheme);
    }
  }, [data, setTheme]);
}

/**
 * Returns a setter that applies the theme locally at once (no flash) and persists it for the user.
 * Used by the theme menu; keeping the write here means the server query cache stays in step with the choice.
 */
export function useSetThemePreference(): (theme: ThemePreference) => void {
  const { setTheme } = useTheme();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (theme: ThemePreference) => saveThemeSetting(theme),
    onSuccess: (settings) =>
      queryClient.setQueryData(THEME_QUERY_KEY, settings),
  });

  return (theme: ThemePreference) => {
    setTheme(theme);
    mutation.mutate(theme);
  };
}
