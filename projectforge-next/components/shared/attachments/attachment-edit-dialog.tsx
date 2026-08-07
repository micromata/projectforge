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
import { Spinner } from "@/components/shared/spinner";
import { AttachmentMetadata } from "./attachment-metadata";
import type { Attachment } from "@/lib/rs/attachments";

export interface AttachmentEditDialogProps {
  attachment: Attachment;
  saving?: boolean;
  onSave: (name: string, description: string) => void;
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
 * Not offered yet: encrypting the file (`AttachmentsServicesRest.encrypt`/`testDecryption`) — see
 * MIGRATION.md, "Offen: Verschlüsselung eines Anhangs".
 */
export function AttachmentEditDialog({
  attachment,
  saving,
  onSave,
  onClose,
}: AttachmentEditDialogProps) {
  const t = useTranslations();
  const ids = useId();
  const [name, setName] = useState(attachment.name);
  const [description, setDescription] = useState(attachment.description ?? "");

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
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
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            {t("cancel")}
          </Button>
          <Button
            type="button"
            // An empty name would leave the file unreachable in the list.
            disabled={name.trim().length === 0 || saving}
            onClick={() => onSave(name.trim(), description.trim())}
          >
            {saving && <Spinner className="h-4 w-4 border-2" />}
            {t("save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
