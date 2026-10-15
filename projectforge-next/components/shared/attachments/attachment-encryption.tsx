"use client";

import { useId, useState, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { FieldHint } from "@/components/shared/form/field-hint";
import { Spinner } from "@/components/shared/spinner";
import { cn } from "@/lib/utils";
import type { Attachment, EncryptionMode } from "@/lib/rs/attachments";
import {
  AttachmentEncryptionMode,
  DEFAULT_ENCRYPTION_MODE,
} from "./attachment-encryption-mode";

export interface AttachmentEncryptionProps {
  attachment: Attachment;
  /** A password call is running — the button shows it, and no second one can be started. */
  pending?: boolean;
  /**
   * Encrypts the file. Resolves to the backend's message for the password field if it refused the
   * password, to null otherwise (the caller closes the dialog then, or toasts a refusal).
   */
  onEncrypt: (password: string, mode: EncryptionMode) => Promise<string | null>;
  /** Tests the password against the encrypted file; resolves like [onEncrypt]. */
  onTestDecryption: (password: string) => Promise<string | null>;
}

/**
 * The encryption of one attachment, as the legacy dialog had it
 * (`AttachmentPageRest.addShowEncryptionOption`):
 *
 * - A plain file can be replaced by a password protected ZIP of it. Folded behind a checkbox, since it
 *   is rare and the dialog is about the name first; the question before it says the password is not
 *   stored, which is the one thing a user must know before losing access to the file's content.
 * - An encrypted file always shows its password field, to test a password against it — there is no
 *   decrypting, the file is downloaded and opened with the password elsewhere.
 *
 * The password never leaves this component except into the one call; it is not kept anywhere else.
 */
export function AttachmentEncryption({
  attachment,
  pending,
  onEncrypt,
  onTestDecryption,
}: AttachmentEncryptionProps) {
  const t = useTranslations();
  const ids = useId();
  const encrypted = !!attachment.encrypted;
  const [open, setOpen] = useState(encrypted);
  const [mode, setMode] = useState<EncryptionMode>(DEFAULT_ENCRYPTION_MODE);
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const canRun = password.length > 0 && !pending;

  async function run(action: () => Promise<string | null>) {
    setError(null);
    setError(await action());
  }

  function start() {
    if (!canRun) return;
    if (encrypted) void run(() => onTestDecryption(password));
    else setConfirming(true);
  }

  // Return in the password field means this section's button, never the dialog's "Save" — saving the
  // name is not what someone typing a password asks for.
  function onPasswordKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== "Enter") return;
    event.preventDefault();
    event.stopPropagation();
    start();
  }

  const passwordLabel = t("password._");

  return (
    <div className="flex flex-col gap-3">
      {!encrypted && (
        <div className="flex items-center gap-2">
          <Checkbox
            id={`${ids}-show`}
            checked={open}
            onCheckedChange={(checked) => setOpen(checked === true)}
          />
          <Label htmlFor={`${ids}-show`}>
            {t("attachment.showEncryptionOption")}
          </Label>
        </div>
      )}

      {open && (
        <div className="grid grid-cols-1 items-end gap-3 sm:grid-cols-[1fr_1fr_auto]">
          {!encrypted && (
            <AttachmentEncryptionMode
              id={`${ids}-mode`}
              value={mode}
              onChange={setMode}
            />
          )}
          <div
            className={cn("flex flex-col gap-2", encrypted && "sm:col-span-2")}
          >
            <div className="flex items-center gap-1.5">
              <Label htmlFor={`${ids}-password`}>{passwordLabel}</Label>
              <FieldHint
                hint={t("attachment.password.info")}
                label={passwordLabel}
              />
            </div>
            <Input
              id={`${ids}-password`}
              type="password"
              autoComplete="off"
              value={password}
              aria-invalid={!!error}
              aria-describedby={error ? `${ids}-error` : undefined}
              onChange={(e) => {
                setPassword(e.target.value);
                setError(null);
              }}
              onKeyDown={onPasswordKeyDown}
            />
          </div>
          <Button
            type="button"
            variant="secondary"
            disabled={!canRun}
            onClick={start}
          >
            {pending && <Spinner className="h-4 w-4 border-2" />}
            {encrypted
              ? t("attachment.testDecryption._")
              : t("attachment.encrypt._")}
          </Button>
          {error && (
            <p
              id={`${ids}-error`}
              className="text-xs text-destructive sm:col-span-3"
            >
              {error}
            </p>
          )}
        </div>
      )}

      {confirming && (
        <ConfirmDialog
          open
          onOpenChange={(next) => !next && setConfirming(false)}
          title={t("attachment.encrypt._")}
          description={t("attachment.encrypt.question")}
          confirmLabel={t("attachment.encrypt._")}
          onConfirm={() => {
            setConfirming(false);
            void run(() => onEncrypt(password, mode));
          }}
        />
      )}
    </div>
  );
}
