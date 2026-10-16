"use client";

import { useEffect, useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import {
  useEntityData,
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import {
  addressImageUrl,
  discardUploadedAddressImage,
  uploadAddressImage,
  uploadedAddressImageUrl,
} from "@/lib/rs/address";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import type { AddressValues } from "./address-schema";
import type { AddressDetail } from "./types";

const ACCEPTED_IMAGES = "image/png,image/jpeg,image/gif";

/**
 * The image of an address: shown, replaced by an upload, or removed — all of it only on save, so a cancel leaves
 * the stored image as it was. An upload waits in the session (`uploadImage/-1`, also for a new address without an
 * id) and is previewed from there; a removal is the flag `imageDeleted`, which the save carries out.
 */
export function AddressImageField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const values = useStore(
    form.store,
    (s: unknown) => (s as { values: AddressValues }).values
  );
  // Saving an image touches the address, so its modification time tells a replaced image from the cached one.
  const version = useEntityData<AddressDetail>()?.lastUpdate ?? "";
  const [uploading, setUploading] = useState(false);

  // An upload left in the session by a form that was abandoned must not end up in this address on save — unless
  // it is this form's own (a remount while one is pending).
  useEffect(() => {
    if ((form.state.values as AddressValues).imageDataUrl == null) {
      void discardUploadedAddressImage().catch(() => undefined);
    }
  }, [form]);

  const src =
    values.imageDataUrl ??
    (values.hasImage && !values.imageDeleted && values.id != null
      ? `${addressImageUrl(values.id)}?v=${encodeURIComponent(version)}`
      : null);

  const upload = async (file: File) => {
    setUploading(true);
    try {
      await uploadAddressImage(file);
      form.setFieldValue("imageDataUrl", uploadedAddressImageUrl(Date.now()));
      form.setFieldValue("imageDeleted", false);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    } finally {
      setUploading(false);
    }
  };

  const remove = () => {
    form.setFieldValue("imageDataUrl", null);
    form.setFieldValue("imageDeleted", true);
    void discardUploadedAddressImage().catch(() => undefined);
  };

  return (
    <div
      className={cn(
        "flex flex-col gap-3 sm:flex-row sm:items-start",
        className
      )}
    >
      {src && (
        <div className="flex flex-col items-start gap-2">
          {/* A plain img: served by the backend from the database or the session, nothing next/image could optimize
              under the static export. */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={src}
            alt={t("address.image")}
            className="max-h-48 max-w-48 rounded-md border object-contain"
          />
          {!readOnly && (
            <Button type="button" variant="outline" size="sm" onClick={remove}>
              <HugeiconsIcon icon={Delete02Icon} />
              {t("delete")}
            </Button>
          )}
        </div>
      )}
      {!readOnly && (
        <AttachmentDropArea
          onFiles={(files) => files[0] && void upload(files[0])}
          accept={ACCEPTED_IMAGES}
          multiple={false}
          disabled={uploading}
          label={t("address.image")}
        />
      )}
    </div>
  );
}
