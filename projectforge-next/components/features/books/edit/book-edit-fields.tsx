"use client";

import type { ReactNode } from "react";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useBookEditForm } from "./book-edit-context";
import type { BookEditValues } from "./book-edit-schema";

type Path = keyof BookEditValues;

interface BaseProps {
  name: Path;
  label: string;
  required?: boolean;
  hint?: string;
  className?: string;
}

interface InputFieldProps extends BaseProps {
  type?: "text" | "date";
  placeholder?: string;
}

function fieldErrors(meta: { errors?: unknown[] }): string[] {
  return (meta.errors ?? [])
    .map((e) => {
      if (e == null) return null;
      if (typeof e === "string") return e;
      if (typeof e === "object" && "message" in e)
        return String((e as { message?: unknown }).message ?? "");
      return null;
    })
    .filter((m): m is string => !!m);
}

function FieldShell({
  label,
  required,
  hint,
  invalid,
  errors,
  className,
  children,
}: {
  label: string;
  required?: boolean;
  hint?: string;
  invalid: boolean;
  errors: string[];
  className?: string;
  children: ReactNode;
}) {
  return (
    <Field data-invalid={invalid || undefined} className={cn("gap-1.5", className)}>
      <FieldLabel className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
        {required && <span className="ml-0.5 text-primary">*</span>}
      </FieldLabel>
      {children}
      {hint && !invalid && <FieldDescription>{hint}</FieldDescription>}
      {invalid && errors.length > 0 && (
        <FieldError>{errors.join(". ")}</FieldError>
      )}
    </Field>
  );
}

export function InputField({
  name,
  label,
  required,
  hint,
  className,
  type = "text",
  placeholder,
}: InputFieldProps) {
  const form = useBookEditForm();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as { isTouched: boolean; isValid: boolean; errors?: unknown[] };
        const invalid = meta.isTouched && !meta.isValid;
        const raw = field.state.value as string | null;
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta)}
            className={className}
          >
            <Input
              type={type}
              placeholder={placeholder}
              value={raw ?? ""}
              onChange={(e) => field.handleChange(e.target.value || null)}
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

interface TextAreaFieldProps extends BaseProps {
  rows?: number;
}

export function TextAreaField({
  name,
  label,
  required,
  hint,
  className,
  rows = 4,
}: TextAreaFieldProps) {
  const form = useBookEditForm();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as { isTouched: boolean; isValid: boolean; errors?: unknown[] };
        const invalid = meta.isTouched && !meta.isValid;
        const raw = field.state.value as string | null;
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta)}
            className={className}
          >
            <Textarea
              rows={rows}
              value={raw ?? ""}
              onChange={(e) => field.handleChange(e.target.value || null)}
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectFieldProps extends BaseProps {
  options: SelectOption[];
}

export function SelectField({
  name,
  label,
  required,
  hint,
  className,
  options,
}: SelectFieldProps) {
  const form = useBookEditForm();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as { isTouched: boolean; isValid: boolean; errors?: unknown[] };
        const invalid = meta.isTouched && !meta.isValid;
        const raw = (field.state.value as string | null) ?? "";
        return (
          <FieldShell
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta)}
            className={className}
          >
            <Select
              value={raw}
              onValueChange={(v) => field.handleChange(v || null)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {options.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
