"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { rawRequest } from "@/lib/rs/client";
import { responseBlob } from "@/lib/rs/download";
import type { ScriptDownload, ScriptExecuteEndpoint } from "@/lib/rs/script";

const IMAGE_TYPES: Record<string, string> = {
  png: "image/png",
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
};

/** The mime type of a result file the browser shows as an image, null for any other file. */
function imageType(filename: string | null | undefined): string | null {
  const extension = filename?.split(".").pop()?.toLowerCase() ?? "";
  return IMAGE_TYPES[extension] ?? null;
}

/**
 * A chart (or any other image) a script produced, shown in place; the download button stays for saving it.
 * Fetched like the download, as a blob url (`img-src blob:` is in the CSP).
 */
export function ScriptResultImage({
  endpoint,
  download,
}: {
  endpoint: ScriptExecuteEndpoint;
  download: ScriptDownload | null;
}) {
  const type = imageType(download?.filename);
  const image = useQuery({
    // The file name carries the timestamp of the execution.
    queryKey: ["script", endpoint, "resultImage", download?.filename],
    queryFn: async ({ signal }) => {
      const res = await rawRequest(
        `/rs/${endpoint}/download`,
        { method: "GET" },
        signal
      );
      if (!res.ok) throw new Error(String(res.status));
      const blob = await responseBlob(res);
      return URL.createObjectURL(new Blob([blob], { type: type! }));
    },
    enabled: !!type,
    staleTime: Infinity,
    gcTime: 0,
  });
  const url = image.data;
  useEffect(() => () => (url ? URL.revokeObjectURL(url) : undefined), [url]);
  if (!type || !url) return null;
  return (
    <div className="flex justify-center rounded-md border bg-white p-2">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={url}
        alt={download?.filename ?? ""}
        className="h-auto max-w-full"
      />
    </div>
  );
}
