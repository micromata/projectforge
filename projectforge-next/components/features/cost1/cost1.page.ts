import { KOST1_METADATA } from "@/lib/metadata/kost1.generated";
import { definePage } from "@/lib/page-def/define-page";
import { CostNumberField } from "./cost-number-field";
import { cost1Schema, COST1_FIELDS, type Cost1Values } from "./cost1-schema";
import { emptyCost1Values, toFormValues } from "./cost1-values";
import type { Cost1Detail, Cost1ListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const COST1_LIST_QUERY_KEY = ["cost1"] as const;

/**
 * The whole cost 1 page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are the three of `Kost1PagesRest.createListLayout`, in its order; their labels, the
 * status texts and every rule come from Kost1DO through the generated metadata. What is declared here
 * is nothing but order, width and the one field the declaration cannot describe: the number, whose
 * four parts are one control (see CostNumberField).
 *
 * One section and no attachments: the entity has no `jcrPath`, so there is nothing to attach. Its
 * change history is a tab of its own, which nothing here says — `KOST1_METADATA.historizable` does
 * (Kost1DO's own `@WithHistory` is commented out, but `DefaultBaseDO` brings one).
 */
export const COST1_PAGE = definePage<
  Cost1ListRow,
  Cost1Values,
  Cost1Detail,
  typeof KOST1_METADATA
>({
  entity: "cost1",
  metadata: KOST1_METADATA,
  route: "/cost1",
  queryKey: COST1_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Cost (MenuCreator, MenuItemDefId.COST).
  categoryKey: "menu.fibu.kost",
  titleKey: "fibu.kost1.title.list._",
  searchPlaceholderKey: "cost1.searchPlaceholder",
  columns: [
    // Filtered as text: the formatted number reads as one ("6.100.01.02"), not as four values.
    // Sorting is the backend's, which maps this property onto the four number columns it is made of
    // (Kost1PagesRest.postProcessMagicFilter) — no column of its own holds it.
    {
      name: "formattedNumber",
      size: 120,
      className: "font-mono font-semibold",
    },
    { name: "kostentraegerStatus", size: 110 },
    { name: "description", size: 400 },
    // When an entry was created and last changed — the two the legacy list omits, but which say
    // whether a cost unit is still being maintained.
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: cost1Schema,
    fieldNames: COST1_FIELDS,
    defaultValues: emptyCost1Values,
    toFormValues,
    // The number is what identifies a cost unit — the same string the list shows.
    title: (cost1) => cost1.formattedNumber ?? "",
    newTitleKey: "fibu.kost1.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        // The entity's own name, not "Kostenträger": that is the label of the number group inside
        // the card (as Wicket's fieldset is), and repeating it as the card's heading read twice.
        titleKey: "fibu.kost1._",
        fields: [
          { custom: CostNumberField, span: 3 },
          // The one value a reader looks for first — whether the cost unit is still in use.
          { name: "kostentraegerStatus", emphasized: true },
          { name: "description", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
