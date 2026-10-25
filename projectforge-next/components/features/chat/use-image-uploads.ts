"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import { uploadChatImage } from "@/lib/rs/chat";
import type { PendingImage } from "./types";

const IMAGE_TYPES = ["image/png", "image/jpeg", "image/gif", "image/webp"];

/**
 * The images of the message being written (ATT-01): each is uploaded at once, the message refers to their ids.
 * Type, size and number are checked here for a quick answer; the server checks them again (by content).
 */
export function useImageUploads(
  conversationId: number,
  maxImages: number,
  maxSize: number
) {
  const t = useTranslations();
  const [images, setImages] = useState<PendingImage[]>([]);
  const counter = useRef(0);
  const imagesRef = useRef(images);
  useEffect(() => {
    imagesRef.current = images;
  }, [images]);

  const patch = useCallback((key: string, change: Partial<PendingImage>) => {
    setImages((list) =>
      list.map((it) => (it.key === key ? { ...it, ...change } : it))
    );
  }, []);

  const add = useCallback(
    (files: File[]) => {
      const free = maxImages - imagesRef.current.length;
      if (files.length > free) {
        toast.error(t("chat.image.error.tooMany", { arg0: maxImages }));
      }
      files.slice(0, Math.max(free, 0)).forEach((file) => {
        if (!IMAGE_TYPES.includes(file.type)) {
          toast.error(t("chat.image.error.unsupportedType"));
          return;
        }
        if (file.size > maxSize) {
          toast.error(
            t("chat.image.error.tooLarge", {
              arg0: Math.round(maxSize / 1024 / 1024),
            })
          );
          return;
        }
        const key = `img-${++counter.current}`;
        const image: PendingImage = {
          key,
          name: file.name,
          previewUrl: URL.createObjectURL(file),
          fileId: null,
          error: null,
        };
        setImages((list) => [...list, image]);
        uploadChatImage(conversationId, file).then(
          (result) => patch(key, { fileId: result.fileId }),
          (error: Error) => patch(key, { error: error.message })
        );
      });
    },
    [conversationId, maxImages, maxSize, patch, t]
  );

  const remove = useCallback((key: string) => {
    setImages((list) => {
      list
        .filter((it) => it.key === key)
        .forEach((it) => URL.revokeObjectURL(it.previewUrl));
      return list.filter((it) => it.key !== key);
    });
  }, []);

  const clear = useCallback(() => {
    setImages((list) => {
      list.forEach((it) => URL.revokeObjectURL(it.previewUrl));
      return [];
    });
  }, []);

  useEffect(
    () => () =>
      imagesRef.current.forEach((it) => URL.revokeObjectURL(it.previewUrl)),
    []
  );

  const uploading = images.some((it) => !it.fileId && !it.error);
  const fileIds = images.flatMap((it) => (it.fileId ? [it.fileId] : []));
  return { images, add, remove, clear, uploading, fileIds };
}
