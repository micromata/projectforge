import { MEMO_METADATA } from "@/lib/metadata/memo.generated";
import { definePage } from "@/lib/page-def/define-page";
import { memoSchema, MEMO_FIELDS, type MemoValues } from "./memo-schema";
import { emptyMemoValues, toFormValues } from "./memo-values";
import type { MemoDetail, MemoListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const MEMO_LIST_QUERY_KEY = ["memo"] as const;

/**
 * The whole page of the memo plugin — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the removed React pages (`MemoPagesRest`), which it follows: the list's columns in their order,
 * the form with subject and text. Memos are personal: everybody sees and writes only the own ones
 * (`MemoRight`). A memo has no history, so its delete is for good.
 */
export const MEMO_PAGE = definePage<
  MemoListRow,
  MemoValues,
  MemoDetail,
  typeof MEMO_METADATA
>({
  entity: "memo",
  metadata: MEMO_METADATA,
  route: "/memo",
  queryKey: MEMO_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (MemoPlugin, MenuItemDefId.MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.memo.title.list",
  defaultSort: { id: "lastUpdate", desc: true },
  columns: [
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
    { name: "subject", size: 260, className: "font-semibold" },
    { name: "memo", size: 500, wrap: true },
  ],
  edit: {
    schema: memoSchema,
    fieldNames: MEMO_FIELDS,
    defaultValues: emptyMemoValues,
    toFormValues,
    title: (memo) => memo.subject ?? "",
    newTitleKey: "plugins.memo.title.add",
    savedMessageKey: "message.successfullChanged",
    autoFocus: "subject",
    sections: [
      {
        id: "general",
        titleKey: "plugins.memo.title.heading",
        fields: [
          { name: "subject", span: 3 },
          { name: "memo", span: 3, rows: 12 },
        ],
      },
    ],
  },
});
