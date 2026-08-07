"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { TextAreaField } from "../book-edit-fields";

export function NotizenSection() {
  const t = useTranslations("books.edit");
  // The labels are the backend's own, as BookDO declares them: `book.abstract` and `comment`
  // (see its @PropertyInfo). `comment` carries no hint — it is an ordinary field, visible to
  // whoever may see the book.
  const tBook = useTranslations("book");
  const tCommon = useTranslations();
  return (
    <SectionCard>
      <SectionHeader title={t("sections.notes")} />
      <div className="flex flex-col gap-4">
        <TextAreaField name="abstractText" label={tBook("abstract")} rows={4} />
        <TextAreaField name="comment" label={tCommon("comment")} rows={3} />
      </div>
    </SectionCard>
  );
}
