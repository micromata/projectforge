"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CropIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

/** The shown image of an address, with the buttons to edit and to remove it unless the form is read-only. */
export function AddressImagePreview({
  src,
  readOnly,
  editDisabled,
  onEdit,
  onRemove,
}: {
  src: string;
  readOnly: boolean;
  editDisabled: boolean;
  onEdit: () => void;
  onRemove: () => void;
}) {
  const t = useTranslations();
  return (
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
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={editDisabled}
            onClick={onEdit}
          >
            <HugeiconsIcon icon={CropIcon} />
            {t("edit")}
          </Button>
          <Button type="button" variant="outline" size="sm" onClick={onRemove}>
            <HugeiconsIcon icon={Delete02Icon} />
            {t("delete")}
          </Button>
        </div>
      )}
    </div>
  );
}
