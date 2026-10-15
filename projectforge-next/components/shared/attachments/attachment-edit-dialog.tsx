"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import { AttachmentEncryption } from "./attachment-encryption";
import { AttachmentFileActions } from "./attachment-file-actions";
import { AttachmentMetadata } from "./attachment-metadata";
import type { Attachment, EncryptionMode } from "@/lib/rs/attachments";

export interface AttachmentEditDialogProps {
  attachment: Attachment;
  /** The entity the file belongs to — for the download link. */
  entity: string;
  id: number;
  saving?: boolean;
  /** Some write on this file is running — no second one can be started from here. */
  busy?: boolean;
  /**
   * Offers encrypting the file and testing a password against it (see AttachmentEncryption). Off
   * where the backend gives no such option either: the Merlin templates, the public data transfer.
   */
  encryptionSupport?: boolean;
  /** A password call is running. */
  encrypting?: boolean;
  onSave: (name: string, description: string) => void;
  /** Asks before deleting (see AttachmentFiles), so this only opens the question. */
  onDelete: () => void;
  onEncrypt: (password: string, mode: EncryptionMode) => Promise<string | null>;
  onTestDecryption: (password: string) => Promise<string | null>;
  onClose: () => void;
}

/**
 * The details of one attachment: its name and description are editable — the only two fields that
 * are (`AttachmentsServicesRest.modify` sends both, so both are always submitted) — everything
 * below them is what the backend recorded (see AttachmentMetadata).
 *
 * A dialog rather than inline fields: the row is narrow, and renaming is rare enough that it should
 * not cost the list a permanent second input per file. The legacy page opened a modal too.
 *
 * Everything else the legacy dialog offered (`AttachmentPageRest.createAttachmentLayout`) is here as
 * well: encrypting the file or testing a password against it (AttachmentEncryption), deleting and
 * downloading it. The row has the last two already, but whoever opened the details to look at a file
 * should not have to close them to act on it.
 */
export function AttachmentEditDialog({
  attachment,
  entity,
  id,
  saving,
  busy,
  encryptionSupport = true,
  encrypting,
  onSave,
  onDelete,
  onEncrypt,
  onTestDecryption,
  onClose,
}: AttachmentEditDialogProps) {
  const t = useTranslations();
  const ids = useId();
  const [name, setName] = useState(attachment.name);
  const [description, setDescription] = useState(attachment.description ?? "");
  const shortcutHint = useSubmitShortcutHint();

  // An empty name would leave the file unreachable in the list — the button's own condition.
  const canSubmit = name.trim().length > 0 && !saving && !busy;
  const onKeyDown = useSubmitShortcut(
    () => onSave(name.trim(), description.trim()),
    canSubmit
  );

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      {/* Wider than the default: the encryption options lay out in one row. */}
      <DialogContent onKeyDown={onKeyDown} className="sm:max-w-2xl">
        <DialogHeader>
          {/* "Anhang", not "Dateiname": the dialog shows all of an attachment, not just its name. */}
          <DialogTitle>{t("attachment._")}</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${ids}-name`}>{t("attachment.fileName")}</Label>
            <Input
              id={`${ids}-name`}
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor={`${ids}-description`}>{t("description")}</Label>
            <Textarea
              id={`${ids}-description`}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
            />
          </div>
          <Separator />
          <AttachmentMetadata attachment={attachment} />
          {encryptionSupport && (
            <>
              <Separator />
              <AttachmentEncryption
                attachment={attachment}
                pending={encrypting}
                onEncrypt={onEncrypt}
                onTestDecryption={onTestDecryption}
              />
            </>
          )}
        </div>

        <DialogFooter className="sm:justify-between">
          <AttachmentFileActions
            fileRef={{ entity, id, fileId: attachment.fileId }}
            busy={busy}
            onDelete={onDelete}
          />
          <div className="flex flex-col-reverse gap-2 sm:flex-row">
            <Button type="button" variant="outline" onClick={onClose}>
              {t("cancel")}
            </Button>
            <HintTooltip {...shortcutHint}>
              <Button
                type="button"
                disabled={!canSubmit}
                onClick={() => onSave(name.trim(), description.trim())}
              >
                {saving && <Spinner className="h-4 w-4 border-2" />}
                {t("save")}
              </Button>
            </HintTooltip>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
