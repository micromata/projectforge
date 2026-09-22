"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { reportMenuUsage } from "@/lib/rs/client";

/**
 * Reports an opened menu entry to the backend, which keeps the history all three frontends share
 * (RecentMenuEntriesService) and sends it back as `MenuData.recentMenu`.
 *
 * Called by the components that render a menu (MainMenuDropdown, FavoritesBar, UserMenu) and by the
 * quick access palette. Deliberately not by MenuLink itself: that one is also used for links which
 * merely look like menu entries (an order in a table cell), and it stays free of state so it costs
 * nothing per row.
 */
export function useReportMenuUsage(): (key: string | undefined) => void {
  const queryClient = useQueryClient();
  const { mutate } = useMutation({
    mutationFn: (key: string) => reportMenuUsage(key),
    // No optimistic update: the palette is closing and the page is navigating away, so nothing is on
    // screen to update, and the refetch lands long before it is opened again.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["menu"] }),
  });
  return (key) => {
    if (key) mutate(key);
  };
}
