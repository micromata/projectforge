"use client";

import { useTranslations } from "next-intl";
import { EditPageTabs } from "@/components/shared/edit-page-tabs";
import { HistorySection } from "@/components/shared/history/history-section";
import { BookEditHeader } from "../edit/book-edit-header";
import { useBookDetail } from "../edit/use-book-detail";
import { bookTabs } from "../book-tabs";

interface Props {
  bookId: number;
}

/**
 * The change history of one book — a page of its own rather than a section of the edit form, so a
 * long history is only built when it is actually looked at.
 *
 * The book itself is read for the header; coming from the form it is already cached.
 */
export function BookHistoryPage({ bookId }: Props) {
  const t = useTranslations("books.edit");
  const { data: book } = useBookDetail(bookId);
  const tabs = bookTabs(bookId, (key) => t(`tabs.${key}`), false);

  return (
    <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
      <div className="shrink-0">
        <BookEditHeader
          title={book?.title ?? ""}
          lendOut={book?.lendOutBy != null}
        />
      </div>
      <EditPageTabs tabs={tabs} activeId="history" />
      <div className="flex-1 overflow-y-auto bg-muted/30 px-6 pb-6 pt-4">
        <HistorySection entity="book" entityId={bookId} />
      </div>
    </div>
  );
}
