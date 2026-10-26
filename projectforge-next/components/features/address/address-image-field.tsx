"use client";

import { useEffect, useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import {
  ImageEditorDialog,
  type EditableImage,
} from "@/components/shared/image-editor/image-editor-dialog";
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
import { AddressImagePreview } from "./address-image-preview";
import type { AddressValues } from "./address-schema";
import type { AddressDetail } from "./types";

const ACCEPTED_IMAGES = "image/png,image/jpeg,image/gif";

/**
 * The image of an address: shown, replaced by an upload, or removed — all of it only on save, so a cancel leaves
 * the stored image as it was. An upload waits in the session (`uploadImage/-1`, also for a new address without an
 * id) and is previewed from there; a removal is the flag `imageDeleted`, which the save carries out. A chosen file
 * and the shown image are cropped and scaled in the image editor first; only its result is uploaded.
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
  const [editing, setEditing] = useState<EditableImage | null>(null);

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

  // A failure is thrown on to the editor, which shows it and stays open with its settings.
  const upload = async (file: File) => {
    setUploading(true);
    try {
      await uploadAddressImage(file);
      form.setFieldValue("imageDataUrl", uploadedAddressImageUrl(Date.now()));
      form.setFieldValue("imageDeleted", false);
    } finally {
      setUploading(false);
    }
  };

  // The object url of a chosen file is released when the editor closes, also by leaving the form.
  const editingSrc = editing?.src;
  useEffect(
    () => () => {
      if (editingSrc?.startsWith("blob:")) URL.revokeObjectURL(editingSrc);
    },
    [editingSrc]
  );

  // `accept` only filters the file chooser: a file dropped in may be anything.
  const edit = (file: File) => {
    if (!ACCEPTED_IMAGES.split(",").includes(file.type)) {
      toast.error(
        t("file.upload.error.unsupportedFormat", { arg0: "PNG, JPEG, GIF" })
      );
      return;
    }
    setEditing({
      src: URL.createObjectURL(file),
      type: file.type,
      name: file.name,
    });
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
        <AddressImagePreview
          src={src}
          readOnly={readOnly}
          editDisabled={uploading}
          onEdit={() => setEditing({ src, name: "image" })}
          onRemove={remove}
        />
      )}
      {!readOnly && (
        <AttachmentDropArea
          onFiles={([file]) => file && edit(file)}
          accept={ACCEPTED_IMAGES}
          multiple={false}
          disabled={uploading}
          label={t("address.image")}
        />
      )}
      {editing && (
        <ImageEditorDialog
          image={editing}
          onApply={upload}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
