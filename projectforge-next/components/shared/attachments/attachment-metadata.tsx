"use client";

import { useTranslations } from "next-intl";
import { CopyButton } from "@/components/shared/copy-button";
import { zipModeMessageKey, type Attachment } from "@/lib/rs/attachments";

interface Props {
  attachment: Attachment;
}

/**
 * What the backend knows about an attachment and the user cannot change: size, encryption status,
 * who created it and who last touched it, and its checksum.
 *
 * Every value arrives ready for display — `sizeHumanReadable`, `createdFormatted` and
 * `lastUpdateTimeAgo` are formatted by the backend in the user's timezone, locale and date format
 * (see `Attachment`), so nothing is re-formatted here. The one exception is the encryption status:
 * the backend only translates it while building the legacy layout, so the key is resolved from
 * `zipMode` (see zipModeMessageKey).
 */
export function AttachmentMetadata({ attachment }: Props) {
  const t = useTranslations();
  const zipMode = zipModeMessageKey(attachment.zipMode);

  return (
    <dl className="grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2">
      <Field
        label={t("attachment.fileSize")}
        value={attachment.sizeHumanReadable}
      />
      <Field
        label={t("attachment.info")}
        // A file that was never encrypted has no zipMode at all; "ohne Verschlüsselung" is the
        // honest reading of that, and the same text the STANDARD mode carries.
        value={t(zipMode ?? "attachment.zip.standard")}
      />
      <Field label={t("created")} value={attachment.createdFormatted} />
      <Field label={t("createdBy")} value={attachment.createdByUser} />
      <Field label={t("modified")} value={attachment.lastUpdateTimeAgo} />
      <Field label={t("modifiedBy")} value={attachment.lastUpdateByUser} />
      {attachment.checksum && (
        <div className="sm:col-span-2">
          <Field
            label={t("attachment.checksum")}
            value={attachment.checksum}
            copyable
          />
        </div>
      )}
    </dl>
  );
}

function Field({
  label,
  value,
  copyable,
}: {
  label: string;
  value?: string | null;
  copyable?: boolean;
}) {
  return (
    <div className="min-w-0">
      <dt className="text-[11px] text-muted-foreground">{label}</dt>
      <dd className="flex items-start gap-1">
        {/* A dash for an empty value, as the legacy ReadonlyField does — an empty line would read
            as a missing field rather than a missing value. */}
        <span className="min-w-0 flex-1 break-all text-xs">{value || "–"}</span>
        {copyable && value && (
          <CopyButton
            value={value}
            label={label}
            variant="ghost"
            className="size-6 text-muted-foreground"
            iconSize={12}
          />
        )}
      </dd>
    </div>
  );
}
