"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon, File01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import type { PendingFile } from "./types";

/** The files of the message being written: images as previews, other files with their name; each removable. */
export function PendingFiles({
  files,
  onRemove,
}: {
  files: PendingFile[];
  onRemove: (key: string) => void;
}) {
  const t = useTranslations();
  if (files.length === 0) return null;
  return (
    <ul className="flex flex-wrap gap-2 px-2 pt-2">
      {files.map((pending) => (
        <li key={pending.key} className="relative">
          {pending.previewUrl ? (
            <div className="size-16 overflow-hidden rounded-md border">
              {/* eslint-disable-next-line @next/next/no-img-element -- local object url, not optimizable */}
              <img
                src={pending.previewUrl}
                alt={pending.file.name}
                className="size-full object-cover"
              />
            </div>
          ) : (
            <div className="flex h-16 max-w-48 items-center gap-1.5 rounded-md border px-2 pr-7 text-xs">
              <HugeiconsIcon icon={File01Icon} className="size-4 shrink-0" />
              <span className="truncate" title={pending.file.name}>
                {pending.file.name}
              </span>
            </div>
          )}
          <Button
            type="button"
            variant="secondary"
            size="icon-xs"
            className="absolute top-0.5 right-0.5"
            aria-label={`${t("chat.file.remove")}: ${pending.file.name}`}
            onClick={() => onRemove(pending.key)}
          >
            <HugeiconsIcon icon={Cancel01Icon} />
          </Button>
        </li>
      ))}
    </ul>
  );
}
