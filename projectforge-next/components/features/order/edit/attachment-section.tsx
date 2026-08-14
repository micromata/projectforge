"use client";

import { AttachmentList } from "@/components/shared/attachments/attachment-list";

/**
 * The attachments of an order — the `UIAttachmentList` the legacy edit layout is reduced to
 * (OrderEntityRest, whose title `attachment.list` the section declares).
 *
 * Nothing but the entity name: attachments are not an order feature, every `AbstractPagesRest` entity
 * can have them (see components/shared/attachments/).
 *
 * @param orderId null for an order being added — nothing can be attached before the first save, since
 * the JCR node hangs off the persisted id.
 */
export function AttachmentSection({ orderId }: { orderId: number | null }) {
  // embedded: inline in the form, so the compact toolbar instead of a permanent drop box.
  return <AttachmentList entity="order" id={orderId} embedded />;
}
