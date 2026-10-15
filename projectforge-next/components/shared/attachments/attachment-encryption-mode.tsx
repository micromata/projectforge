"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import type { EncryptionMode } from "@/lib/rs/attachments";

/**
 * The modes offered, strongest first — its label says "hohe Sicherheit", so it is the default (as in
 * `AttachmentPageRest.addShowEncryptionOption`). The keys are spelled out, typo of the bundle
 * included, so the i18n scan finds them.
 */
const MODES: { mode: EncryptionMode; key: string }[] = [
  { mode: "ENCRYPTED_AES256", key: "attachment.zip.encrytpedAes256" },
  { mode: "ENCRYPTED_STANDARD", key: "attachment.zip.encryptedStandard" },
];

export const DEFAULT_ENCRYPTION_MODE: EncryptionMode = MODES[0].mode;

/** The choice of the algorithm a file is encrypted with (see AttachmentEncryption). */
export function AttachmentEncryptionMode({
  id,
  value,
  onChange,
}: {
  id: string;
  value: EncryptionMode;
  onChange: (mode: EncryptionMode) => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{t("attachment.zip.encryptionAlgorithm")}</Label>
      <Select
        value={value}
        onValueChange={(next) => onChange(next as EncryptionMode)}
      >
        <SelectTrigger id={id} className="w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {MODES.map(({ mode, key }) => (
            <SelectItem key={mode} value={mode}>
              {t(key)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
