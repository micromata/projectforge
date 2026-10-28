import { POSTAUSGANG_METADATA } from "@/lib/metadata/postausgang.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  OutboxContentField,
  OutboxPersonField,
  OutboxReceiverField,
  OutboxSenderField,
} from "./post-suggest-fields";
import { emptyPostausgangValues, toPostausgangValues } from "./post-values";
import {
  postausgangSchema,
  POSTAUSGANG_FIELDS,
  type PostausgangValues,
} from "./postausgang-schema";
import type { PostausgangDetail, PostausgangListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const POSTAUSGANG_LIST_QUERY_KEY = ["outgoingMail"] as const;

/**
 * The outbox ("Postausgang") — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `PostausgangPagesRest`, which it follows: the list's columns (the last
 * update added, hidden by default) and the form's date, type, receiver, person, content, sender and comment,
 * the free texts completing from the stored entries.
 */
export const POSTAUSGANG_PAGE = definePage<
  PostausgangListRow,
  PostausgangValues,
  PostausgangDetail,
  typeof POSTAUSGANG_METADATA
>({
  entity: "outgoingMail",
  metadata: POSTAUSGANG_METADATA,
  route: "/outgoingMail",
  queryKey: POSTAUSGANG_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Organization (MenuCreator, MenuItemDefId.OUTBOX_LIST).
  categoryKey: "menu.orga",
  titleKey: "orga.postausgang.title.list",
  // Newest first, as PostausgangDao orders them.
  defaultSort: { id: "datum", desc: true },
  columns: [
    { name: "datum", size: 110 },
    { name: "empfaenger", size: 240, className: "font-semibold" },
    { name: "person", size: 180 },
    { name: "absender", size: 180 },
    { name: "inhalt", size: 280, wrap: true },
    { name: "bemerkung", size: 320, wrap: true },
    { name: "type", size: 140 },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  edit: {
    schema: postausgangSchema,
    fieldNames: POSTAUSGANG_FIELDS,
    defaultValues: emptyPostausgangValues,
    toFormValues: toPostausgangValues,
    title: (entry) =>
      [entry.empfaenger, entry.inhalt].filter(Boolean).join(": "),
    newTitleKey: "orga.postausgang.title.add",
    savedMessageKey: "message.successfullChanged",
    // Date and type are preset (PostausgangEntityRest.newBaseDO); what the user came to type is the receiver.
    autoFocus: "empfaenger",
    sections: [
      {
        id: "general",
        titleKey: "orga.postausgang.title.heading",
        fields: [
          { name: "datum" },
          { name: "type" },
          { custom: OutboxReceiverField, span: 3 },
          { custom: OutboxPersonField, span: 3 },
          { custom: OutboxContentField, span: 3 },
          { custom: OutboxSenderField, span: 3 },
          { name: "bemerkung", span: 3, rows: 3 },
        ],
      },
    ],
  },
});
