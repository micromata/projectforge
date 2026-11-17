"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  attachmentPreviewUrl,
  fetchAttachmentPreviewText,
  previewKind,
  type Attachment,
  type AttachmentRef,
} from "@/lib/rs/attachments";

/** The most of a text shown; a log of many megabytes would make the page crawl. */
const MAX_TEXT_LENGTH = 256 * 1024;

/** The content of one file in AttachmentViewer, by its [previewKind]; a refused one says so. */
export function AttachmentPreview({
  file,
  fileRef,
}: {
  file: Attachment;
  fileRef: AttachmentRef;
}) {
  const [failed, setFailed] = useState(false);
  const url = attachmentPreviewUrl(fileRef);
  if (failed) return <NotAvailable />;
  switch (previewKind(file)) {
    case "pdf":
      return pdfViewerInPage() ? (
        // Same origin, so allowed by `frame-src` (default-src 'self') and `frame-ancestors 'self'`. The viewer of
        // the browser brings zoom, search and print. A file that turns out not to be a PDF shows the 415 page.
        <iframe src={url} title={file.name} className="size-full border-0" />
      ) : (
        <NotAvailable message="attachment.view.pdfNotSupported" />
      );
    case "image":
      return (
        <div className="flex size-full items-center justify-center p-2">
          {/* eslint-disable-next-line @next/next/no-img-element -- served by Spring with the session */}
          <img
            src={url}
            alt={file.name}
            className="max-h-full max-w-full object-contain"
            onError={() => setFailed(true)}
          />
        </div>
      );
    case "text":
      return <TextPreview fileRef={fileRef} onFailed={() => setFailed(true)} />;
    default:
      return <NotAvailable />;
  }
}

/** Read and shown as text, escaped by React: whatever the file contains, nothing of it is run. */
function TextPreview({
  fileRef,
  onFailed,
}: {
  fileRef: AttachmentRef;
  onFailed: () => void;
}) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ["attachmentPreview", fileRef],
    queryFn: ({ signal }) => fetchAttachmentPreviewText(fileRef, signal),
    retry: false,
    staleTime: 60_000,
  });
  useEffect(() => {
    if (query.isError) onFailed();
  }, [query.isError, onFailed]);
  if (query.data == null) return null;
  const truncated = query.data.length > MAX_TEXT_LENGTH;
  return (
    <div className="p-3">
      {truncated && (
        <p className="mb-2 text-xs text-muted-foreground">
          {t("attachment.view.truncated", { arg0: "256 KB" })}
        </p>
      )}
      <pre className="font-mono text-xs break-all whitespace-pre-wrap">
        {truncated ? query.data.slice(0, MAX_TEXT_LENGTH) : query.data}
      </pre>
    </div>
  );
}

function NotAvailable({
  message = "attachment.view.notAvailable",
}: {
  message?: "attachment.view.notAvailable" | "attachment.view.pdfNotSupported";
}) {
  const t = useTranslations();
  return (
    <div className="flex size-full items-center justify-center p-6 text-center text-sm text-muted-foreground">
      {t(message)}
    </div>
  );
}

/**
 * Whether the browser shows a PDF within the page. Not on phones and tablets: Chrome on Android has no viewer
 * for it, Safari on iOS shows only the first page in a frame — both are offered "open in new tab" and the
 * download instead. `pdfViewerEnabled` tells the first, the second needs the device (an iPad reports itself as
 * a Mac, but with touch points).
 */
function pdfViewerInPage(): boolean {
  if (typeof navigator === "undefined") return true;
  if (navigator.pdfViewerEnabled === false) return false;
  const iOS =
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  return !iOS;
}
