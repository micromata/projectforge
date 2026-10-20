import { POSTEINGANG_METADATA } from "@/lib/metadata/posteingang.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  InboxContentField,
  InboxPersonField,
  InboxSenderField,
} from "./post-suggest-fields";
import { emptyPosteingangValues, toPosteingangValues } from "./post-values";
import {
  posteingangSchema,
  POSTEINGANG_FIELDS,
  type PosteingangValues,
} from "./posteingang-schema";
import type { PosteingangDetail, PosteingangListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const POSTEINGANG_LIST_QUERY_KEY = ["incomingMail"] as const;

/**
 * The inbox ("Posteingang") — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `PosteingangPagesRest`, which it follows: the list's columns (the last
 * update added, hidden by default) and the form's date, type, sender, person, content and comment, the free
 * texts completing from the stored entries.
 */
export const POSTEINGANG_PAGE = definePage<
  PosteingangListRow,
  PosteingangValues,
  PosteingangDetail,
  typeof POSTEINGANG_METADATA
>({
  entity: "incomingMail",
  metadata: POSTEINGANG_METADATA,
  route: "/incomingMail",
  queryKey: POSTEINGANG_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Organization (MenuCreator, MenuItemDefId.INBOX_LIST).
  categoryKey: "menu.orga",
  titleKey: "orga.posteingang.title.list",
  // Newest first, as PosteingangDao orders them.
  defaultSort: { id: "datum", desc: true },
  columns: [
    { name: "datum", size: 110 },
    { name: "absender", size: 240, className: "font-semibold" },
    { name: "person", size: 180 },
    { name: "inhalt", size: 280, wrap: true },
    { name: "bemerkung", size: 320, wrap: true },
    { name: "type", size: 140 },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  edit: {
    schema: posteingangSchema,
    fieldNames: POSTEINGANG_FIELDS,
    defaultValues: emptyPosteingangValues,
    toFormValues: toPosteingangValues,
    title: (entry) => [entry.absender, entry.inhalt].filter(Boolean).join(": "),
    newTitleKey: "orga.posteingang.title.add",
    savedMessageKey: "message.successfullChanged",
    // Date and type are preset (PosteingangEntityRest.newBaseDO); what the user came to type is the sender.
    autoFocus: "absender",
    sections: [
      {
        id: "general",
        titleKey: "orga.posteingang.title.heading",
        fields: [
          { name: "datum" },
          { name: "type" },
          { custom: InboxSenderField, span: 3 },
          { custom: InboxPersonField, span: 3 },
          { custom: InboxContentField, span: 3 },
          { name: "bemerkung", span: 3, rows: 3 },
        ],
      },
    ],
  },
});
