"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { File01Icon, FolderOpenIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { chatFileUrl } from "@/lib/rs/chat";
import type { ChatFile } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";

/**
 * The non-image (or no longer available) files of a message as downloads, each with a link to its data
 * transfer area. A file expired or deleted in the data transfer stays listed as no longer available.
 */
export function MessageFilesList({
  conversationId,
  files,
}: {
  conversationId: number;
  files: ChatFile[];
}) {
  const t = useTranslations();
  if (files.length === 0) return null;
  return (
    <ul className="mt-1 flex flex-wrap gap-2">
      {files.map((file) => {
        const content = (
          <>
            <HugeiconsIcon icon={File01Icon} className="size-4 shrink-0" />
            <span className="min-w-0 truncate">{file.name}</span>
            <span className="shrink-0 text-muted-foreground">
              {file.available
                ? file.sizeFormatted
                : t("chat.file.notAvailable")}
            </span>
          </>
        );
        const className =
          "flex min-w-0 items-center gap-1.5 px-2 py-1.5 text-xs";
        return (
          <li key={file.fileId}>
            {file.available ? (
              <div className="flex max-w-80 items-stretch overflow-hidden rounded-md border">
                <a
                  href={chatFileUrl(conversationId, file.fileId, true)}
                  download={file.name}
                  className={cn(className, "hover:bg-muted")}
                  aria-label={`${t("chat.file.download")}: ${file.name}`}
                >
                  {content}
                </a>
                <HintTooltip text={t("chat.file.openArea")}>
                  <Link
                    href={`/datatransfer/${file.areaId}`}
                    className="flex shrink-0 items-center border-l px-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                    aria-label={`${t("chat.file.openArea")}: ${file.name}`}
                  >
                    <HugeiconsIcon icon={FolderOpenIcon} className="size-4" />
                  </Link>
                </HintTooltip>
              </div>
            ) : (
              <span
                className={cn(
                  className,
                  "max-w-80 rounded-md border text-muted-foreground opacity-70"
                )}
              >
                {content}
              </span>
            )}
          </li>
        );
      })}
    </ul>
  );
}
