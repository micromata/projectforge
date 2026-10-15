import { KOST2_ART_METADATA } from "@/lib/metadata/kost2-art.generated";
import { definePage } from "@/lib/page-def/define-page";
import { Cost2TypeNumberField } from "./cost2-type-number-field";
import {
  cost2TypeSchema,
  COST2_TYPE_FIELDS,
  type Cost2TypeValues,
} from "./cost2-type-schema";
import { emptyCost2TypeValues, toFormValues } from "./cost2-type-values";
import type { Kost2ArtDetail, Kost2ArtListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const COST2_TYPE_LIST_QUERY_KEY = ["cost2Type"] as const;

/**
 * The whole cost-2 type page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are the ones the legacy Wicket `Kost2ArtListPage` shows, in its order; their labels and
 * every rule come from Kost2ArtDO through the generated metadata. The Nummer is the entity's own id
 * shown as the two-digit string it reads as ("05", `Kost2Art.getFormattedId`), sorted by the backend's
 * `id` — it has no field of its own, hence a computed column.
 */
export const COST2_TYPE_PAGE = definePage<
  Kost2ArtListRow,
  Cost2TypeValues,
  Kost2ArtDetail,
  typeof KOST2_ART_METADATA
>({
  entity: "cost2Type",
  metadata: KOST2_ART_METADATA,
  route: "/cost2Type",
  queryKey: COST2_TYPE_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Cost (MenuCreator, MenuItemDefId.COST).
  categoryKey: "menu.fibu.kost",
  titleKey: "fibu.kost2art.title.list",
  defaultSort: { id: "id" },
  columns: [
    {
      id: "id",
      labelKey: "fibu.kost2art.nummer",
      accessor: (row) => row.formattedId ?? String(row.id),
      size: 90,
      className: "font-mono font-semibold",
    },
    { name: "name", size: 260 },
    { name: "fakturiert", size: 110 },
    { name: "workFraction", size: 120 },
    { name: "projektStandard", size: 140 },
    { name: "description", size: 400 },
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: cost2TypeSchema,
    fieldNames: COST2_TYPE_FIELDS,
    defaultValues: emptyCost2TypeValues,
    toFormValues,
    // The Nummer identifies the type; its name is the readable half of the same heading.
    title: (kost2Art) => kost2Art.formattedId ?? kost2Art.name ?? "",
    newTitleKey: "fibu.kost2art.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "fibu.kost2art.title.heading",
        fields: [
          { custom: Cost2TypeNumberField },
          { name: "name", span: 3 },
          { name: "fakturiert" },
          { name: "projektStandard" },
          { name: "workFraction" },
          { name: "description", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
