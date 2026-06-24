"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchMenu } from "@/lib/rs/client";
import type { MenuData } from "@/lib/rs/types";

export function useMenu() {
  return useQuery<MenuData>({
    queryKey: ["menu"],
    queryFn: ({ signal }) => fetchMenu(signal),
    staleTime: 10 * 60 * 1000,
  });
}
