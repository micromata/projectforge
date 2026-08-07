"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { InputField, SelectField } from "../book-edit-fields";
import { useBookStatusOptions, useBookTypeOptions } from "../use-book-options";
import { KeywordsField } from "./keywords-field";

/**
 * The fields of a book, in the order and with the labels of the legacy edit page
 * (BookPagesRest.createEditLayout).
 *
 * Every label is the one BookDO declares in its `@PropertyInfo` — `book.title`, `book.editor`
 * ("Herausgeber:in", not "Auflage"), `status`, `book.type` — so the wording matches the rest of
 * ProjectForge and no text has to be invented here.
 *
 * `status` sits next to `type` rather than in the loan section: it is a property of the book
 * (BookDO: `nullable = false`), not of a loan, and putting it here also means it renders for a new
 * book, whose default is PRESENT.
 */
export function AllgemeinSection() {
  const t = useTranslations("books.edit");
  const tBook = useTranslations("book");
  const tCommon = useTranslations();
  const typeOptions = useBookTypeOptions();
  const statusOptions = useBookStatusOptions();
  return (
    <SectionCard>
      <SectionHeader title={t("sections.general")} />
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <InputField
          name="title"
          label={tBook("title._")}
          required
          className="md:col-span-3"
        />
        <InputField
          name="authors"
          label={tBook("authors")}
          required
          className="md:col-span-3"
        />
        <SelectField
          name="type"
          label={tBook("type._")}
          // Optional in the database (`nullable = true`), so it must be possible to clear again.
          clearable
          options={typeOptions}
        />
        <SelectField
          name="status"
          label={tCommon("status")}
          // Mandatory in the database (BookDO: `nullable = false`), hence no clear option.
          required
          // The one field a reader looks for first — whether the book is there at all.
          emphasized
          options={statusOptions}
        />
        <InputField name="isbn" label={tBook("isbn")} />
        <InputField
          name="yearOfPublishing"
          label={tBook("yearOfPublishing._")}
        />
        <InputField
          name="publisher"
          label={tBook("publisher")}
          className="md:col-span-2"
        />
        <InputField name="signature" label={tBook("signature")} />
        <InputField
          name="editor"
          label={tBook("editor")}
          className="md:col-span-2"
        />
        <KeywordsField className="md:col-span-3" />
      </div>
    </SectionCard>
  );
}
