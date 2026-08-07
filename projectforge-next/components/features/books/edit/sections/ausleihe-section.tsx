"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { InputField, SelectField } from "../book-edit-fields";
import { useBookStatusOptions } from "../use-book-options";
import { LendOutByField } from "./lend-out-by-field";
import { AusleiheHistoryTable } from "../ausleihe-history-table";
import type { BookDetail } from "../../types";

interface Props {
  book: BookDetail;
}

export function AusleiheSection({ book }: Props) {
  const t = useTranslations("books.edit");
  const statusOptions = useBookStatusOptions();

  return (
    <SectionCard>
      <SectionHeader title={t("sections.loan")} />
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <LendOutByField label={t("fields.lendOutBy")} />
        <InputField
          type="date"
          name="lendOutDate"
          label={t("fields.lendOutDate")}
        />
        <InputField name="lendOutComment" label={t("fields.lendOutComment")} />
        <SelectField
          name="status"
          label={t("fields.status")}
          // Mandatory in the database (BookDO: `nullable = false`), so mark it as such here too.
          required
          options={statusOptions}
        />
      </div>
      <div className="mt-6">
        <SectionHeader title={t("sections.loanHistory")} />
        <AusleiheHistoryTable book={book} />
      </div>
    </SectionCard>
  );
}
