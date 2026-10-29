"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import type { ChatConversation } from "@/lib/rs/chat-types";
import type { PendingFile } from "./types";

const PREVIEW_TYPES = ["image/png", "image/jpeg", "image/gif", "image/webp"];

/**
 * The files of the message being written (ATT-01): kept here and sent with the message, so nothing unsent lands in
 * the data transfer (for a direct message: in the personal box of the recipient). Images get a local preview.
 * Number and size are checked here for a quick answer; the server checks them again (the size against the data
 * transfer area). Without files (not allowed by the host, no data transfer), a dropped or pasted file only gets the
 * hint saying which.
 */
export function usePendingFiles(conversation: ChatConversation) {
  const t = useTranslations();
  const [files, setFiles] = useState<PendingFile[]>([]);
  const counter = useRef(0);
  const filesRef = useRef(files);
  useEffect(() => {
    filesRef.current = files;
  }, [files]);
  const {
    filesEnabled,
    filesAllowed,
    maxFilesPerMessage,
    maxFileSize,
    maxFileSizeFormatted,
  } = conversation;

  const add = useCallback(
    (added: File[]) => {
      if (added.length === 0) return;
      if (!filesEnabled) {
        toast.error(
          t(
            filesAllowed
              ? "chat.file.error.unavailable"
              : "chat.file.error.notAllowed"
          )
        );
        return;
      }
      const free = maxFilesPerMessage - filesRef.current.length;
      if (added.length > free) {
        toast.error(t("chat.file.error.tooMany", { arg0: maxFilesPerMessage }));
      }
      const accepted = added.slice(0, Math.max(free, 0)).filter((file) => {
        if (maxFileSize != null && file.size > maxFileSize) {
          toast.error(
            t("chat.file.error.tooLarge", {
              arg0: file.name,
              arg1: maxFileSizeFormatted ?? "",
            })
          );
          return false;
        }
        return true;
      });
      setFiles((list) => [
        ...list,
        ...accepted.map((file) => ({
          key: `file-${++counter.current}`,
          file,
          previewUrl: PREVIEW_TYPES.includes(file.type)
            ? URL.createObjectURL(file)
            : null,
        })),
      ]);
    },
    [
      filesEnabled,
      filesAllowed,
      maxFilesPerMessage,
      maxFileSize,
      maxFileSizeFormatted,
      t,
    ]
  );

  const revoke = (list: PendingFile[]) =>
    list.forEach((it) => it.previewUrl && URL.revokeObjectURL(it.previewUrl));

  const remove = useCallback((key: string) => {
    setFiles((list) => {
      revoke(list.filter((it) => it.key === key));
      return list.filter((it) => it.key !== key);
    });
  }, []);

  const clear = useCallback(() => {
    setFiles((list) => {
      revoke(list);
      return [];
    });
  }, []);

  useEffect(() => () => revoke(filesRef.current), []);

  /** All files together must fit into one request. */
  const tooLarge =
    files.reduce((sum, it) => sum + it.file.size, 0) >
    conversation.maxRequestSize;
  return { files, add, remove, clear, tooLarge };
}
