"use client";

import {
  isServer,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { RsError } from "@/lib/rs/client";

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
        // Off by default on purpose: an edit form reads its entity through a query too, and a refetch on
        // focus would overwrite what the user has begun to type (the "new" preset spells this out in
        // useEntityDetail). Lists have the opposite need — pick up a write made in another tab or the
        // legacy UI on return — and turn it on for themselves (refetchOnWindowFocus: "always" in
        // useMagicFilterQuery). Kept narrow rather than global for that reason.
        refetchOnWindowFocus: false,
        // A refusal will not become an answer by asking again, and the three default attempts would
        // hold up what the page does about it — for a denied read that is a redirect, so the user
        // would sit in front of a spinner for seconds first (see useReadAccessGuard). The 403s that
        // *are* worth repeating never reach here: rawRequest retries them itself.
        retry: (failureCount, error) =>
          error instanceof RsError &&
          (error.status === 401 || error.status === 403)
            ? false
            : failureCount < 3,
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

export function getQueryClient() {
  if (isServer) return makeQueryClient();
  if (!browserQueryClient) browserQueryClient = makeQueryClient();
  return browserQueryClient;
}

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const queryClient = getQueryClient();
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
