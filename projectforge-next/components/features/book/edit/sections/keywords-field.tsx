"use client";

import { useTranslations } from "next-intl";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { TagInput } from "@/components/shared/tag-input";
import { cn } from "@/lib/utils";
import { useEntityEditForm } from "@/components/shared/form/form-context";

function parse(raw: string | null): string[] {
  if (!raw) return [];
  return raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function serialize(tags: string[]): string | null {
  return tags.length === 0 ? null : tags.join(", ");
}

export function KeywordsField({ className }: { className?: string }) {
  const t = useTranslations("books.edit");
  // BookDO's own label (`book.keywords`); the hint below it is ours — how the tag input confirms an
  // entry has no backend counterpart.
  const label = useTranslations("book")("keywords");
  const form = useEntityEditForm();
  return (
    <form.Field name={"keywords" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const tags = parse(field.state.value as string | null);
        return (
          <Field className={cn("gap-1.5", className)}>
            <FieldLabel className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
              {label}
            </FieldLabel>
            <TagInput
              value={tags}
              onChange={(next) => field.handleChange(serialize(next))}
              variant="primary"
              inputAriaLabel={label}
            />
            <FieldDescription>{t("fields.keywordsHint")}</FieldDescription>
          </Field>
        );
      }}
    </form.Field>
  );
}
