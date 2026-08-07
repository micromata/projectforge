"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";

interface Props {
  /** null for a book being added — nothing can be attached before the first save. */
  bookId: number | null;
}

/**
 * The attachments of a book — the legacy page's `UIAttachmentList` (BookPagesRest, with
 * `attachment.list` as its title).
 *
 * Everything but the title lives in `components/shared/attachments/`: attachments are not a book
 * feature, every `AbstractPagesRest` entity can have them.
 */
export function AttachmentSection({ bookId }: Props) {
  const t = useTranslations();
  return (
    <SectionCard>
      <SectionHeader title={t("attachment.list")} />
      <AttachmentList entity="book" id={bookId} />
    </SectionCard>
  );
}
