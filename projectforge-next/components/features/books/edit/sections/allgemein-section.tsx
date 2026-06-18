"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import {
  InputField,
  SelectField,
  type SelectOption,
} from "../book-edit-fields";
import { KeywordsField } from "./keywords-field";

const TYPE_OPTIONS: SelectOption[] = [
  { value: "BOOK", label: "Buch" },
  { value: "MAGAZINE", label: "Magazin" },
  { value: "EBOOK", label: "E-Book" },
  { value: "OTHER", label: "Sonstiges" },
];

export function AllgemeinSection() {
  const t = useTranslations("books.edit");
  const f = (k: string) => t(`fields.${k}`);
  return (
    <SectionCard>
      <SectionHeader title={t("sections.general")} />
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <InputField
          name="title"
          label={f("title")}
          required
          className="md:col-span-3"
        />
        <InputField name="signature" label={f("signature")} />
        <InputField name="yearOfPublishing" label={f("yearOfPublishing")} />
        <SelectField name="type" label="Typ" options={TYPE_OPTIONS} />
        <InputField
          name="publisher"
          label={f("publisher")}
          className="md:col-span-2"
        />
        <InputField name="isbn" label={f("isbn")} />
        <InputField name="editor" label={f("editor")} />
        <InputField
          name="authors"
          label={f("authors")}
          required
          className="md:col-span-2"
        />
        <KeywordsField className="md:col-span-3" />
      </div>
    </SectionCard>
  );
}
