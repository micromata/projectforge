"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { TextAreaField } from "../book-edit-fields";

export function NotizenSection() {
  const t = useTranslations("books.edit");
  return (
    <SectionCard>
      <SectionHeader title={t("sections.notes")} />
      <div className="flex flex-col gap-4">
        <TextAreaField name="abstractText" label={t("fields.abstract")} rows={4} />
        <TextAreaField
          name="comment"
          label={t("fields.comment")}
          hint={t("fields.commentHint")}
          rows={3}
        />
      </div>
    </SectionCard>
  );
}
