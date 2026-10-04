"use client";

import { useQuery } from "@tanstack/react-query";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { fetchDatevImportLogViewerUrl } from "@/lib/rs/datev-import";

/**
 * "View log" of the DATEV import, as the former Wicket page offered it: opens the log viewer with the
 * user's import log (upload, faulty rows, reconcile, commit progress) in a new tab, so the import page with its
 * preview or running job stays as it is.
 *
 * The url is fetched on mount, which also registers the log subscription early; the backend ensures it on every
 * import request anyway, so nothing is lost if this request comes late.
 */
export function DatevImportLogViewerLink() {
  const { data: url } = useQuery({
    queryKey: ["datev-import", "logViewer"],
    queryFn: ({ signal }) => fetchDatevImportLogViewerUrl(signal),
    staleTime: Infinity,
  });
  return <LogViewerLink url={url} />;
}
