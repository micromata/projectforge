"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { InputField } from "../book-edit-fields";
import { useBookEditForm } from "../book-edit-context";
import { BookLoanActions } from "./book-loan-actions";
import type { UserRef } from "../../types";

/**
 * The loan of a book: who has it since when, an optional note, and the two actions that change it
 * (BookLoanActions).
 *
 * `lendOutBy` and `lendOutDate` are shown, not edited: both are set by the server from the session
 * and the current date (BookServicesRest), so an input for them could only offer a user the backend
 * cannot resolve. Only `lendOutComment` is the user's to write — hence `book.lendOutNote`
 * ("Ausleihnotiz (optional)").
 *
 * There is no loan history — BookDO stores only the *current* loan, and nothing in the backend
 * records past ones. What past loans there are show up as `lendOutBy` changes in the entity's
 * change history, which has a tab of its own (see bookTabs).
 *
 * `status` is not here but in the general section: it describes the book, not the loan (see
 * GeneralSection).
 */
export function LoanSection() {
  const t = useTranslations("book");
  const format = useFormatContext();
  const form = useBookEditForm();

  // Read from the form, not from the fetched book: after a loan action the form is reset onto the
  // server's values (see BookEditForm), and this way the line follows along.
  const values = useStore(form.store, (s: unknown) => (s as FormState).values);
  const lendOutBy: UserRef | null = values.lendOutBy;

  return (
    <SectionCard>
      <SectionHeader title={t("lending")} />
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <div className="flex flex-col gap-1.5">
          <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
            {t("lendOutBy")}
          </span>
          <span className="text-sm">
            {lendOutBy
              ? // Date only: lendOutDate is a LocalDate. The legacy component formatted it with
                // jsTimestampFormatMinutes and so rendered a meaningless 00:00.
                [lendOutBy.displayName, formatDate(values.lendOutDate, format)]
                  .filter(Boolean)
                  .join(", ")
              : // Not lent out — a dash, so the line reads as "nobody" and keeps its height.
                "–"}
          </span>
        </div>
        <InputField
          name="lendOutComment"
          label={t("lendOutNote")}
          className="md:col-span-2"
        />
      </div>
      <BookLoanActions />
    </SectionCard>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (book-edit-context). */
interface FormState {
  values: { lendOutBy: UserRef | null; lendOutDate: string | null };
}
