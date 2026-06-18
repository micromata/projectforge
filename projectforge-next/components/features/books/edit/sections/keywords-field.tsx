"use client";

import { useTranslations } from "next-intl";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { TagInput } from "@/components/shared/tag-input";
import { cn } from "@/lib/utils";
import { useBookEditForm } from "../book-edit-context";

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
  const form = useBookEditForm();
  return (
    <form.Field name={"keywords" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const tags = parse(field.state.value as string | null);
        return (
          <Field className={cn("gap-1.5", className)}>
            <FieldLabel className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
              {t("fields.keywords")}
            </FieldLabel>
            <TagInput
              value={tags}
              onChange={(next) => field.handleChange(serialize(next))}
              variant="primary"
              inputAriaLabel={t("fields.keywords")}
            />
            <FieldDescription>{t("fields.keywordsHint")}</FieldDescription>
          </Field>
        );
      }}
    </form.Field>
  );
}
