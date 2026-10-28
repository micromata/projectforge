"use client";

import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DateInput } from "@/components/shared/date-input";
import { FormAlert } from "@/components/shared/form-alert";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { MarkdownText } from "@/components/shared/markdown-text";
import { Spinner } from "@/components/shared/spinner";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { NumberBox } from "@/components/shared/form/number-box";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import { RsError } from "@/lib/rs/client";
import type { ValidationError } from "@/lib/rs/types";
import type {
  ValidityEntry,
  ValidityPrefill,
  ValidityValueField,
  ValidityWriteResult,
} from "./types";

export interface ValidityEntryDialogProps<T extends ValidityEntry> {
  /** The entry to edit, or a fresh one (`id` null) to add. */
  entry: T;
  title: string;
  dateField: string;
  dateLabelKey: string;
  valueFields: readonly ValidityValueField[];
  onSave: (entry: T) => Promise<ValidityWriteResult<T>>;
  /** Absent for a new entry. Asks before deleting. */
  onDelete?: (entry: T) => Promise<ValidityWriteResult<T>>;
  /** After a successful write, with the entries the backend answered with. */
  onDone: (entries: T[]) => void;
  onClose: () => void;
  /**
   * Proposes values for a new entry: when the dialog opens and whenever its date changes. Not asked for a stored
   * entry, whose values are the user's.
   */
  prefill?: (entry: T, date: string) => Promise<ValidityPrefill<T> | null>;
}

/**
 * Adds, edits or deletes one entry with a validity period: its start date, its values and a comment.
 *
 * Holds the values itself (not a form of the page): an entry is written on its own, through its own
 * endpoint, while the form around it may hold unsaved changes. What is wrong with the values is the
 * backend's to say — its validation errors are shown at the field they name, the others above the fields.
 */
export function ValidityEntryDialog<T extends ValidityEntry>({
  entry,
  title,
  dateField,
  dateLabelKey,
  valueFields,
  onSave,
  onDelete,
  onDone,
  onClose,
  prefill,
}: ValidityEntryDialogProps<T>) {
  const t = useTranslations();
  const [values, setValues] = useState<Record<string, unknown>>(() => ({
    ...(entry as Record<string, unknown>),
  }));
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [rejected, setRejected] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const shortcutHint = useSubmitShortcutHint();
  // Only the answer to the latest date counts: an earlier one may arrive after it.
  const prefillRequest = useRef(0);

  const set = (name: string, value: unknown) =>
    setValues((prev) => ({ ...prev, [name]: value }));

  // Without a date (cleared, or not a date yet) there is nothing to ask for.
  const runPrefill = (date: string | null) => {
    if (!prefill || entry.id != null || !date) return;
    const request = ++prefillRequest.current;
    prefill(entry, date)
      .then((result) => {
        if (request !== prefillRequest.current) return;
        if (result?.values) {
          setValues((prev) => ({ ...prev, ...result.values }));
        }
        setInfo(result?.info ?? null);
      })
      // Nothing proposed: the user types the values.
      .catch(() => {
        if (request === prefillRequest.current) setInfo(null);
      });
  };

  useEffect(() => {
    runPrefill(
      ((entry as Record<string, unknown>)[dateField] as string | null) ?? null
    );
    // Once, for the entry the dialog opened with; later dates come through the date field.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const errorsOf = (name: string) =>
    errors.filter((e) => e.fieldId === name).map((e) => e.message);
  const fieldNames = new Set([
    dateField,
    "comment",
    ...valueFields.map((f) => f.name),
  ]);
  const globalErrors = errors.filter(
    (e) => !e.fieldId || !fieldNames.has(e.fieldId)
  );

  const run = async (write: () => Promise<ValidityWriteResult<T>>) => {
    setPending(true);
    setErrors([]);
    setRejected(null);
    try {
      const result = await write();
      if (result.kind === "ok") {
        onDone(result.entries);
      } else if (result.kind === "validationErrors") {
        setErrors(result.validationErrors);
      } else {
        setRejected(result.message);
      }
    } catch (error) {
      // A refused write (no write access: 403) or a failure of the call; the dialog stays open.
      setRejected(
        error instanceof RsError && error.status === 403
          ? t("access.exception.noAccess")
          : t("errorpage.title")
      );
    } finally {
      setPending(false);
    }
  };

  const save = () => run(() => onSave(values as T));
  const canSubmit = !pending;
  const submitShortcut = useSubmitShortcut(save, canSubmit);
  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    // Return on one of the dialog's own buttons (cancel, delete) is that button's click, not a save.
    if (event.target instanceof HTMLButtonElement) return;
    submitShortcut(event);
  };

  return (
    // The dialogs are portals, but React bubbles their events along the component tree — into the edit
    // form around this section, whose own Return shortcut would save the employee (and leave the page)
    // while the confirmation or a write of this dialog is open. Stopped here, for both dialogs.
    <div className="contents" onKeyDown={(event) => event.stopPropagation()}>
      <Dialog open onOpenChange={(open) => !open && onClose()}>
        {/* Radix focuses the first field when the dialog opens, and focusing [DateInput] opens its
            calendar — over the fields below it. The dialog opens with nothing focused instead (as the
            forecast export does); Tab reaches the date. */}
        <DialogContent
          onOpenAutoFocus={(event) => event.preventDefault()}
          onKeyDown={onKeyDown}
        >
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
          </DialogHeader>

          <div className="flex flex-col gap-4">
            {rejected && <FormAlert tone="error">{rejected}</FormAlert>}
            {globalErrors.length > 0 && (
              <FormAlert tone="error">
                {globalErrors.map((e) => e.message).join(". ")}
              </FormAlert>
            )}
            <DateField
              name={dateField}
              label={t(dateLabelKey)}
              value={values[dateField] as string | null | undefined}
              errors={errorsOf(dateField)}
              onChange={(value) => {
                set(dateField, value);
                if (value !== values[dateField]) runPrefill(value);
              }}
            />
            {valueFields.map((field) => (
              <ValueField
                key={field.name}
                field={field}
                value={values[field.name]}
                errors={errorsOf(field.name)}
                onChange={(value) => set(field.name, value)}
              />
            ))}
            {info && (
              <FormAlert tone="info">
                <MarkdownText text={info} />
              </FormAlert>
            )}
            <CommentField
              value={(values.comment as string | null | undefined) ?? ""}
              errors={errorsOf("comment")}
              onChange={(value) => set("comment", value || null)}
            />
          </div>

          <DialogFooter className="sm:justify-between">
            <div>
              {onDelete && (
                <Button
                  type="button"
                  variant="destructive"
                  disabled={pending}
                  onClick={() => setConfirmDelete(true)}
                >
                  {t("delete")}
                </Button>
              )}
            </div>
            <div className="flex flex-col-reverse gap-2 sm:flex-row">
              <Button type="button" variant="outline" onClick={onClose}>
                {t("cancel")}
              </Button>
              <HintTooltip {...shortcutHint}>
                <Button type="button" disabled={!canSubmit} onClick={save}>
                  {pending && <Spinner className="h-4 w-4 border-2" />}
                  {t("save")}
                </Button>
              </HintTooltip>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      {onDelete && (
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={setConfirmDelete}
          title={t("delete")}
          description={t("question.markAsDeletedQuestion")}
          confirmLabel={t("delete")}
          destructive
          onConfirm={() => {
            setConfirmDelete(false);
            void run(() => onDelete(entry));
          }}
        />
      )}
    </div>
  );
}

function DateField({
  name,
  label,
  value,
  errors,
  onChange,
}: {
  name: string;
  label: string;
  value: string | null | undefined;
  errors: string[];
  onChange: (value: string | null) => void;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell
      name={name}
      label={label}
      required
      invalid={errors.length > 0}
      errors={errors}
      ids={ids}
    >
      <DateInput
        id={ids.controlId}
        value={value}
        onChange={onChange}
        invalid={errors.length > 0}
      />
    </FieldShell>
  );
}

function ValueField({
  field,
  value,
  errors,
  onChange,
}: {
  field: ValidityValueField;
  value: unknown;
  errors: string[];
  onChange: (value: unknown) => void;
}) {
  const t = useTranslations();
  const ids = useFieldIds();
  const invalid = errors.length > 0;
  const label = field.label ?? t(field.labelKey);
  return (
    <FieldShell
      name={field.name}
      label={label}
      required={field.required}
      invalid={invalid}
      errors={errors}
      ids={ids}
    >
      {field.kind === "select" ? (
        <Select
          value={value == null ? "" : String(value)}
          onValueChange={(it) => onChange(it || null)}
        >
          <SelectTrigger
            id={ids.controlId}
            aria-labelledby={ids.labelId}
            aria-invalid={invalid || undefined}
            className="w-full"
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {field.options?.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {t(option.labelKey)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      ) : field.kind === "text" ? (
        <Input
          id={ids.controlId}
          value={value == null ? "" : String(value)}
          aria-invalid={invalid || undefined}
          onChange={(e) => onChange(e.target.value || null)}
        />
      ) : (
        <NumberBox
          id={ids.controlId}
          value={toNumber(value)}
          onChange={onChange}
          fractionDigits={field.kind === "int" ? 0 : undefined}
          suffix={field.suffix}
          invalid={invalid}
          aria-label={label}
        />
      )}
    </FieldShell>
  );
}

function CommentField({
  value,
  errors,
  onChange,
}: {
  value: string;
  errors: string[];
  onChange: (value: string) => void;
}) {
  const t = useTranslations();
  const ids = useFieldIds();
  return (
    <FieldShell
      name="comment"
      label={t("comment")}
      invalid={errors.length > 0}
      errors={errors}
      ids={ids}
    >
      <Textarea
        id={ids.controlId}
        value={value}
        rows={3}
        onChange={(e) => onChange(e.target.value)}
      />
    </FieldShell>
  );
}

/** A number value may come as a string off the wire (a generic attribute value). */
export function toNumber(value: unknown): number | null {
  if (value == null || value === "") return null;
  const numeric = typeof value === "number" ? value : Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}
