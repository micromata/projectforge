import { useQuery } from "@tanstack/react-query";
import type { GroupNomenclatureConfig } from "@/lib/group-nomenclature";
import { request } from "@/lib/rs/client";

/** Shared, so a change saved on the configuration page can invalidate it. */
export const GROUP_NOMENCLATURE_QUERY_KEY = ["groupNomenclature"] as const;

/**
 * The configured naming convention of groups (`GroupEntityRest.nomenclature`), for the wizards'
 * suggestions and the hint at a group's name. Undefined while loading or if the request fails — the
 * callers then behave as without a configuration.
 */
export function useGroupNomenclature(): GroupNomenclatureConfig | undefined {
  return useQuery({
    queryKey: GROUP_NOMENCLATURE_QUERY_KEY,
    queryFn: ({ signal }) =>
      request<GroupNomenclatureConfig>(
        "/rs/group/nomenclature",
        { method: "GET" },
        signal
      ),
    // Fetched again whenever a page using it opens: an admin may just have changed it in another tab.
    staleTime: 0,
    retry: false,
  }).data;
}
