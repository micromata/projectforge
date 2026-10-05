import { KOST2_METADATA } from "@/lib/metadata/kost2.generated";
import { definePage } from "@/lib/page-def/define-page";
import { Cost2NumberField } from "./cost2-number-field";
import { Cost2ProjectField } from "./cost2-project-field";
import { Cost2SharedCostField } from "./cost2-shared-cost-field";
import { cost2Schema, COST2_FIELDS, type Cost2Values } from "./cost2-schema";
import { emptyCost2Values, toFormValues } from "./cost2-values";
import { Cost2ListActions } from "./cost2-list-actions";
import type { Cost2Detail, Cost2ListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const COST2_LIST_QUERY_KEY = ["cost2"] as const;

/**
 * The whole cost 2 page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are those the legacy Wicket `Kost2ListPage` shows, in its order; the scalar labels, the
 * status texts and every rule come from Kost2DO through the generated metadata. The nested columns
 * (the Kost2Art and the project/customer) carry their own label and sort path, because the metadata
 * only covers scalar Kost2DO fields. The number is declared as one control (see Cost2NumberField), the
 * project selection fills its first three parts (see Cost2ProjectField).
 */
export const COST2_PAGE = definePage<
  Cost2ListRow,
  Cost2Values,
  Cost2Detail,
  typeof KOST2_METADATA
>({
  entity: "cost2",
  metadata: KOST2_METADATA,
  route: "/cost2",
  queryKey: COST2_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Cost (MenuCreator, MenuItemDefId.COST).
  categoryKey: "menu.fibu.kost",
  titleKey: "fibu.kost2.title.list",
  columns: [
    // Filtered as text: the formatted number reads as one ("6.100.01.02"), not as four values. Sorting
    // is the backend's, which maps this property onto the columns it is made of
    // (Kost2EntityRest.postProcessMagicFilter) — no column of its own holds it.
    {
      name: "formattedNumber",
      size: 120,
      className: "font-mono font-semibold",
    },
    // The Kost2Art the number's last part names, and whether its costs are invoiced.
    {
      id: "kost2Art.name",
      labelKey: "fibu.kost2.art",
      accessor: (row) => row.kost2Art?.name ?? "",
      referenceKey: "kost2Art",
      size: 140,
    },
    {
      id: "kost2Art.fakturiert",
      labelKey: "fibu.fakturiert",
      accessor: (row) => row.kost2Art?.fakturiert ?? null,
      dataType: "BOOLEAN",
      size: 100,
    },
    { name: "workFraction", size: 110 },
    { name: "sharedCost", size: 110, hiddenByDefault: true },
    // The project and its customer the cost unit belongs to; sorted by the entity's own paths.
    {
      id: "projekt.kunde.name",
      labelKey: "fibu.kunde._",
      accessor: (row) => row.project?.customer?.name ?? "",
      referenceKey: "project",
      size: 180,
    },
    {
      id: "projekt.name",
      labelKey: "fibu.projekt._",
      accessor: (row) => row.project?.name ?? "",
      referenceKey: "project",
      size: 180,
    },
    { name: "kostentraegerStatus", size: 110 },
    { name: "description", size: 300 },
    { name: "comment", size: 300 },
    // When an entry was created and last changed — the two the legacy list omits, but which say
    // whether a cost unit is still being maintained.
    { name: "created", size: 130, hiddenByDefault: true },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  listActions: Cost2ListActions,
  // Mass update of a selection: status, shared cost, description and comment (Kost2MultiSelectedPageRest, /cost2Selected).
  massUpdate: {
    endpoint: "cost2Selected",
    route: "/cost2/mass-update",
  },
  edit: {
    schema: cost2Schema,
    fieldNames: COST2_FIELDS,
    defaultValues: emptyCost2Values,
    toFormValues,
    // The number is what identifies a cost unit — the same string the list shows.
    title: (cost2) => cost2.formattedNumber ?? "",
    newTitleKey: "fibu.kost2.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        // The entity's own name, not "Kostenträger": that is the label of the number group inside the
        // card, and repeating it as the card's heading reads twice.
        titleKey: "fibu.kost2._",
        fields: [
          // First, so picking a project fills the number's first three parts (Cost2ProjectField).
          { custom: Cost2ProjectField, span: 3 },
          { custom: Cost2NumberField, span: 3 },
          { name: "workFraction" },
          { custom: Cost2SharedCostField },
          // The one value a reader looks for first — whether the cost unit is still in use.
          { name: "kostentraegerStatus", emphasized: true },
          { name: "description", span: 3, rows: 4 },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
