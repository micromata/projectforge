"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDataTransferVerticalIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { useEntityData } from "@/components/shared/form/form-context";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import {
  deleteLicenseFile,
  downloadLicenseFile,
  fetchLicenseFiles,
  licenseFilesQueryKey,
  swapLicenseFiles,
  uploadLicenseFile,
  type LicenseFileSlot as Slot,
  type LicenseFilesState,
} from "@/lib/rs/license";
import { LicenseFileSlot } from "./license-file-slot";
import type { LicenseDetail } from "./types";

/** The two slots with their labels — spelled out, so the i18n key scan of the generator finds them. */
const SLOTS: readonly { slot: Slot; labelKey: string }[] = [
  { slot: 1, labelKey: "plugins.licensemanagement.file1" },
  { slot: 2, labelKey: "plugins.licensemanagement.file2" },
];

/**
 * The two stored files of a license and the button swapping them — Wicket's file upload panels of
 * `LicenseEditForm`. Each write goes straight to the backend (lib/rs/license.ts), not through the save:
 * the files are no part of the DTO.
 *
 * For a stored license only, the same reason attachments say so in their own words: the files hang off
 * the license row, which needs the persisted id. The section holding this field is dropped for a user
 * who may not see the key (see license.page.ts), as Wicket dropped the panels.
 */
export function LicenseFilesField({ className }: { className?: string }) {
  const t = useTranslations();
  const qc = useQueryClient();
  const licenseId = useEntityData<LicenseDetail>()?.id ?? null;
  const enabled = licenseId != null && licenseId > 0;

  const files = useQuery({
    queryKey: licenseFilesQueryKey(licenseId),
    queryFn: ({ signal }) => fetchLicenseFiles(licenseId!, signal),
    enabled,
  });

  const applyState = (state: LicenseFilesState) =>
    qc.setQueryData(licenseFilesQueryKey(licenseId), state);
  // The backend's own text for a refusal (too large), which lib/rs/license.ts carries.
  const onError = (error: unknown) =>
    toast.error(error instanceof Error ? error.message : String(error));

  const upload = useMutation({
    mutationFn: ({ slot, file }: { slot: Slot; file: File }) =>
      uploadLicenseFile(licenseId!, slot, file),
    onSuccess: applyState,
    onError,
  });
  const remove = useMutation({
    mutationFn: (slot: Slot) => deleteLicenseFile(licenseId!, slot),
    onSuccess: applyState,
    onError,
  });
  const swap = useMutation({
    mutationFn: () => swapLicenseFiles(licenseId!),
    onSuccess: applyState,
    onError,
  });

  if (!enabled) {
    return (
      <p className={cn("text-sm text-muted-foreground", className)}>
        {t("attachment.onlyAvailableAfterSave")}
      </p>
    );
  }

  const busy = upload.isPending || remove.isPending || swap.isPending;
  const state = files.data;
  const filenameOf = (slot: Slot) =>
    slot === 1 ? state?.filename1 : state?.filename2;

  return (
    <div className={className}>
      <div className="flex flex-col gap-2">
        {SLOTS.map(({ slot, labelKey }) => (
          <LicenseFileSlot
            key={slot}
            label={t(labelKey)}
            filename={filenameOf(slot)}
            busy={busy || files.isLoading}
            uploading={upload.isPending && upload.variables?.slot === slot}
            removing={remove.isPending && remove.variables === slot}
            onDownload={() =>
              downloadLicenseFile(licenseId, slot).catch(onError)
            }
            onUpload={(file) => upload.mutate({ slot, file })}
            onRemove={() => remove.mutate(slot)}
          />
        ))}
        {(state?.filename1 || state?.filename2) && (
          <div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-7 gap-1.5 text-[11px]"
              disabled={busy}
              onClick={() => swap.mutate()}
            >
              <HugeiconsIcon icon={ArrowDataTransferVerticalIcon} size={13} />
              {t("plugins.licensemanagement.swapFiles")}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
