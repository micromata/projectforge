"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, FingerPrintIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { GuardedDialog } from "@/components/shared/guarded-dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import {
  deleteWebAuthnEntry,
  fetchWebAuthnRegisterOptions,
  finishWebAuthnRegister,
  updateWebAuthnEntry,
} from "@/lib/rs/my-2fa-setup";
import { toast } from "@/lib/toast";
import { isWebAuthnSupported, register } from "@/lib/webauthn";
import { HELP_TEXT_CLASS } from "./help-text";
import type { My2FASetupInit, My2FAWebAuthnEntry } from "./types";
import { runSetupWrite } from "./write-result";

/**
 * The WebAuthn keys of the user (FIDO2 tokens, passkeys): listed, registered in a dialog, renamed or deleted
 * in a dialog of their row (the legacy page `react/WebAuthnSetup/dynamic`). Registering and deleting need a
 * recent 2FA.
 */
export function WebAuthnSection({
  init,
  onChanged,
  check2FA,
}: {
  init: My2FASetupInit;
  onChanged: () => Promise<void>;
  /** Asks for the second factor now; false if cancelled. */
  check2FA: () => Promise<boolean>;
}) {
  const t = useTranslations();
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<My2FAWebAuthnEntry | null>(null);
  const supported = isWebAuthnSupported();

  return (
    <SectionCard>
      <SectionHeader
        title={t("webauthn.title")}
        trailing={
          supported && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="gap-1.5"
              onClick={() => setAdding(true)}
            >
              <HugeiconsIcon icon={Add01Icon} size={14} aria-hidden />
              {t("add")}
            </Button>
          )
        }
      />
      <div className="flex flex-col gap-3 text-sm">
        <p className={HELP_TEXT_CLASS}>{t("webauthn.info")}</p>
        {!init.fresh2FA && (
          <FormAlert tone="info">
            {t("webauthn.registration.2FARequired.info")}
          </FormAlert>
        )}
        {init.webAuthnEntries.length > 0 && (
          <div className="overflow-x-auto rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("webauthn.entry.displayName._")}</TableHead>
                  <TableHead>{t("created")}</TableHead>
                  <TableHead>{t("lastUpdate")}</TableHead>
                  <TableHead className="text-right">
                    {t("webauthn.entry.signCount._")}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {init.webAuthnEntries.map((entry) => (
                  <TableRow
                    key={entry.id}
                    className="cursor-pointer"
                    onClick={() => setEditing(entry)}
                  >
                    <TableCell>
                      <button
                        type="button"
                        className="text-left text-primary hover:underline"
                        onClick={(event) => {
                          event.stopPropagation();
                          setEditing(entry);
                        }}
                      >
                        {entry.displayName || t("untitled")}
                      </button>
                    </TableCell>
                    <TableCell>{entry.createdFormatted}</TableCell>
                    <TableCell>{entry.lastUpdateFormatted}</TableCell>
                    <TableCell className="text-right">
                      {entry.signCount ?? 0}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
      {adding && (
        <RegisterDialog
          fresh2FA={init.fresh2FA}
          check2FA={check2FA}
          onClose={() => setAdding(false)}
          onRegistered={onChanged}
        />
      )}
      {editing && (
        <EditDialog
          entry={editing}
          onClose={() => setEditing(null)}
          onChanged={onChanged}
        />
      )}
    </SectionCard>
  );
}

function RegisterDialog({
  fresh2FA,
  check2FA,
  onClose,
  onRegistered,
}: {
  fresh2FA: boolean;
  check2FA: () => Promise<boolean>;
  onClose: () => void;
  onRegistered: () => Promise<void>;
}) {
  const t = useTranslations();
  const [displayName, setDisplayName] = useState("");
  const [running, setRunning] = useState(false);

  const run = async () => {
    setRunning(true);
    try {
      // Up front: the challenge is spent by the attempt, so a 403 of the finish couldn't simply be repeated.
      if (!fresh2FA && !(await check2FA())) return;
      const options = await fetchWebAuthnRegisterOptions();
      const finishRequest = await register(options);
      if (!finishRequest) return; // Cancelled in the browser's prompt.
      const registered = await runSetupWrite(() =>
        finishWebAuthnRegister(displayName, finishRequest)
      );
      if (registered) {
        toast.success(t("operation.updated"));
        await onRegistered();
        onClose();
      }
    } catch (error) {
      // E.g. InvalidStateError: the key is registered already (excludeCredentials).
      toast.error(error instanceof Error ? error.message : String(error));
    } finally {
      setRunning(false);
    }
  };

  return (
    <GuardedDialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("webauthn.title")}</DialogTitle>
          <DialogDescription>{t("webauthn.info")}</DialogDescription>
        </DialogHeader>
        <form
          className="flex flex-col gap-1.5"
          onSubmit={(event) => {
            event.preventDefault();
            void run();
          }}
        >
          <Label htmlFor="webauthn-register-displayName">
            {t("webauthn.entry.displayName._")}
          </Label>
          <Input
            id="webauthn-register-displayName"
            value={displayName}
            placeholder={t("untitled")}
            onChange={(event) => setDisplayName(event.target.value)}
          />
          <p className="text-xs text-muted-foreground">
            {t("webauthn.entry.displayName.info")}
          </p>
          <DialogFooter className="mt-3">
            <Button type="button" variant="outline" onClick={onClose}>
              {t("cancel")}
            </Button>
            <Button type="submit" disabled={running} className="gap-1.5">
              <HugeiconsIcon icon={FingerPrintIcon} size={14} aria-hidden />
              {t("webauthn.registration.button.register")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </GuardedDialog>
  );
}

function EditDialog({
  entry,
  onClose,
  onChanged,
}: {
  entry: My2FAWebAuthnEntry;
  onClose: () => void;
  onChanged: () => Promise<void>;
}) {
  const t = useTranslations();
  const [displayName, setDisplayName] = useState(entry.displayName ?? "");
  const [busy, setBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const write = async (
    action: () => ReturnType<typeof deleteWebAuthnEntry>,
    message: string
  ) => {
    setBusy(true);
    try {
      if (await runSetupWrite(action)) {
        toast.success(message);
        await onChanged();
        onClose();
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <GuardedDialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("webauthn.entry.edit")}</DialogTitle>
          <DialogDescription className="sr-only">
            {t("webauthn.entry.displayName.info")}
          </DialogDescription>
        </DialogHeader>
        <form
          className="flex flex-col gap-1.5"
          onSubmit={(event) => {
            event.preventDefault();
            void write(
              () => updateWebAuthnEntry(entry.id, displayName),
              t("operation.updated")
            );
          }}
        >
          <Label htmlFor="webauthn-edit-displayName">
            {t("webauthn.entry.displayName._")}
          </Label>
          <Input
            id="webauthn-edit-displayName"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
          />
          <p className="text-xs text-muted-foreground">
            {t("webauthn.entry.displayName.info")}
          </p>
          {!!entry.signCount && (
            <p className="text-xs text-muted-foreground">
              {t("webauthn.entry.signCount._")}: {entry.signCount}
            </p>
          )}
          <DialogFooter className="mt-3 sm:justify-between">
            <Button
              type="button"
              variant="outline"
              className="text-destructive"
              disabled={busy}
              onClick={() => setConfirmDelete(true)}
            >
              {t("delete")}
            </Button>
            <div className="flex gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                {t("cancel")}
              </Button>
              <Button type="submit" disabled={busy}>
                {t("update")}
              </Button>
            </div>
          </DialogFooter>
        </form>
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={setConfirmDelete}
          title={t("webauthn.entry.edit")}
          description={t("webauthn.entry.delete.question")}
          confirmLabel={t("delete")}
          destructive
          onConfirm={() =>
            void write(
              () => deleteWebAuthnEntry(entry.id),
              t("operation.deleted")
            )
          }
        />
      </DialogContent>
    </GuardedDialog>
  );
}
