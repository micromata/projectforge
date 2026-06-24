"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchUserStatus } from "@/lib/rs/client";
import type { UserStatus } from "@/lib/rs/types";

export function useAuth() {
  const query = useQuery<UserStatus>({
    queryKey: ["userStatus"],
    queryFn: ({ signal }) => fetchUserStatus(signal),
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

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
