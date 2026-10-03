import { CONFIGURATION_METADATA } from "@/lib/metadata/configuration.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  configurationSchema,
  CONFIGURATION_FIELDS,
  type ConfigurationValues,
} from "./schema";
import { emptyConfigurationValues, toFormValues } from "./values";
import { ConfigurationAccessNote } from "./edit/configuration-access-note";
import { ConfigurationValueField } from "./edit/configuration-value-field";
import { ConfigurationValueCell } from "./list/configuration-value-cell";
import { ParamLabelCell } from "./list/param-label-cell";
import type { ConfigurationDetail, ConfigurationRow } from "./types";

/** REST category of the configuration entity — `ConfigurationEntityRest` is mapped to "configuration". */
export const CONFIGURATION_ENTITY = "configuration";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const CONFIGURATION_LIST_QUERY_KEY = ["configuration"] as const;
/** Route of the list; the edit page hangs below it as `/configuration/{id}`. */
export const CONFIGURATION_ROUTE = "/configuration";

/**
 * The system-configuration page — list and per-parameter edit — as data (see lib/page-def/types.ts),
 * the successor of the Wicket `ConfigurationListPage` / `ConfigurationEditPage`. Each row is one
 * `ConfigurationParam` of the fixed set; opening it edits the one value the parameter's
 * `configurationType` calls for (see [ConfigurationValueField]).
 *
 * Hand-built rather than server-laid-out because the value is polymorphic — one of four typed slots
 * chosen by the type — which no single-type `UILayout` describes; the metadata carries only the base
 * columns for the same reason (no `@PropertyInfo` on the DO's value). The parameter set is fixed:
 * `ConfigurationDao` refuses insert and delete, so there is no add button and no mass update. The three
 * columns mirror the legacy table (parameter, value, description). The list is sorted by the parameter's
 * displayed label by default (see defaultSort below), so editing a row never reshuffles the table; the
 * value and description columns do not sort (no backing property), and none carries a column filter. Both
 * the label sort and the free-text search run in memory on the backend over the translated label,
 * description and raw name (`ConfigurationEntityRest.computedSortProperties` / `preProcessMagicFilter`),
 * since the shown texts are runtime translations no database column carries.
 */
export const CONFIGURATION_PAGE = definePage<
  ConfigurationRow,
  ConfigurationValues,
  ConfigurationDetail,
  typeof CONFIGURATION_METADATA
>({
  entity: CONFIGURATION_ENTITY,
  metadata: CONFIGURATION_METADATA,
  route: CONFIGURATION_ROUTE,
  queryKey: CONFIGURATION_LIST_QUERY_KEY,
  // Administration > System configuration (MenuCreator, CONFIGURATION).
  categoryKey: "menu.administration",
  // `.title.list`, not the bare `administration.configuration`: that key is both a text and the parent
  // of `param`/`parameter`/`value`, so the generator exports it as `administration.configuration._`
  // (a namespace to `t()`). The list header resolves its key through leafKeyOf, but the edit heading
  // and section titles below are handed to `t()` directly, so all of them name a plain leaf.
  titleKey: "administration.configuration.title.list",
  // By the parameter's displayed label, ascending — a stable order so editing a row never reshuffles the
  // table (the backend returns no inherent order otherwise, so a saved row drifts to where the DB last
  // wrote it). The label is a runtime translation, so the backend sorts it in memory keyed by this column
  // id (ConfigurationEntityRest.computedSortProperties["parameter"]), not by a SQL ORDER BY.
  defaultSort: { id: "parameter" },
  columns: [
    {
      // The parameter, shown as its translated label (the `i18nKey` resolved through next-intl) and sorted
      // by that same label (see defaultSort). No column filter: free-text search matches the label already
      // (ConfigurationEntityRest.preProcessMagicFilter), and a per-column filter over the raw keys would not.
      id: "parameter",
      labelKey: "administration.configuration.parameter",
      accessor: (row) => row.parameter,
      filterKind: null,
      size: 320,
      cell: (ctx) => (
        <ParamLabelCell
          i18nKey={ctx.row.original.i18nKey}
          highlight={ctx.table.options.meta?.highlight}
        />
      ),
    },
    {
      // The current value, read-only, formatted for the type it is stored as (see ConfigurationValueCell).
      id: "value",
      labelKey: "administration.configuration.value",
      accessor: (row) => row.stringValue ?? row.longValue ?? row.floatValue,
      sortable: false,
      filterKind: null,
      size: 320,
      cell: (ctx) => (
        <ConfigurationValueCell
          row={ctx.row.original}
          highlight={ctx.table.options.meta?.highlight}
        />
      ),
    },
    {
      // The parameter's description (its `descriptionI18nKey`), wrapped since it is a sentence.
      id: "description",
      labelKey: "description",
      accessor: (row) => row.descriptionI18nKey,
      sortable: false,
      filterKind: null,
      size: 420,
      wrap: true,
      cell: (ctx) => (
        <ParamLabelCell
          i18nKey={ctx.row.original.descriptionI18nKey}
          highlight={ctx.table.options.meta?.highlight}
        />
      ),
    },
  ],
  // A parameter maintained on a page of its own (the customer groups) opens that page directly; the
  // backend names it only to the parameter's editors, so everyone else gets the read-only edit page.
  onRowClick: (row) => (row.editPage ? `/${row.editPage}` : undefined),
  // No massUpdate and no listActions: the parameter set is fixed and each parameter is edited on its own.
  edit: {
    schema: configurationSchema,
    fieldNames: CONFIGURATION_FIELDS,
    defaultValues: emptyConfigurationValues,
    toFormValues,
    // The parameter's translated label (server-supplied, user locale — see Configuration.label). Falls
    // back to the parameter key, which is never null on a stored row.
    title: (entry) => entry.label ?? entry.parameter ?? "",
    // Never reached (no insert), but the contract requires it — and it is shown while an existing row
    // still loads (id set, data not yet), so it must be a plain leaf `t()` accepts.
    newTitleKey: "administration.configuration.title.edit",
    savedMessageKey: "message.successfullChanged",
    editIntro: ConfigurationAccessNote,
    sections: [
      {
        id: "configuration",
        titleKey: "administration.configuration.title.heading",
        fields: [{ custom: ConfigurationValueField, span: 3 }],
      },
    ],
  },
});
