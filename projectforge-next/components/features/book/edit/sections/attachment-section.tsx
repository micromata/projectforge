"use client";

import { AttachmentList } from "@/components/shared/attachments/attachment-list";

interface Props {
  /** null for a book being added — nothing can be attached before the first save. */
  bookId: number | null;
}

/**
 * The attachments of a book — the legacy page's `UIAttachmentList` (BookPagesRest, whose title
 * `attachment.list` the section declares).
 *
 * Everything but this line lives in `components/shared/attachments/`: attachments are not a book
 * feature, every `AbstractPagesRest` entity can have them. What remains is the entity name — hence a
 * section body of the book's own rather than a declared field.
 */
export function AttachmentSection({ bookId }: Props) {
  return <AttachmentList entity="book" id={bookId} />;
}
