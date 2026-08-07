"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { InputField } from "../book-edit-fields";
import { LendOutByField } from "./lend-out-by-field";

/**
 * The loan of a book: exactly the three fields BookDO holds for it, with its own labels
 * (`book.lending` as the title, `book.lendOutBy`, `date` for lendOutDate, `book.lendOutNote` for
 * lendOutComment).
 *
 * There is no loan history — BookDO stores only the *current* loan, and nothing in the backend
 * records past ones. What past loans there are show up as `lendOutBy` changes in the entity's
 * change history, which has a tab of its own (see bookTabs).
 *
 * `status` is not here but in the general section: it describes the book, not the loan (see
 * GeneralSection).
 */
export function LoanSection() {
  const tBook = useTranslations("book");
  const tCommon = useTranslations();

  return (
    <SectionCard>
      <SectionHeader title={tBook("lending")} />
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <LendOutByField label={tBook("lendOutBy")} />
        {/* "date._" and not "date": the key carries a subtree (date.begin, date.end …), which
            next-intl reads as a namespace, so the bare label lives under "_". */}
        <InputField type="date" name="lendOutDate" label={tCommon("date._")} />
        <InputField name="lendOutComment" label={tBook("lendOutNote")} />
      </div>
    </SectionCard>
  );
}
