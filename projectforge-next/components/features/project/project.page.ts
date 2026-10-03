import { PROJEKT_METADATA } from "@/lib/metadata/projekt.generated";
import { definePage } from "@/lib/page-def/define-page";
import { ProjectCustomerField } from "./project-customer-field";
import { ProjectKontoField } from "./project-konto-field";
import { ProjectKost2TypesField } from "./project-kost2-types-field";
import { ProjectNumberField } from "./project-number-field";
import {
  projectSchema,
  PROJECT_FIELDS,
  type ProjectValues,
} from "./project-schema";
import { emptyProjectValues, toFormValues } from "./project-values";
import type { ProjectDetail, ProjectListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const PROJECT_LIST_QUERY_KEY = ["project"] as const;

/**
 * The whole project page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are those of the former `ProjectPagesRest.createListLayout` in its order, the form is
 * Wicket's `ProjektEditForm`; every scalar label, the status texts and every rule come from ProjektDO
 * through the generated metadata. Declared here is order and width, plus what the declaration cannot
 * describe: the cost number (see ProjectNumberField), the customer and the account (foreign DOs without
 * metadata) and the cost 2 types, of which a newly checked one becomes a cost 2 unit on save
 * (see ProjectKost2TypesField).
 *
 * The list starts with the projects not ended — the backend's `listType` filter, which replaces the
 * status filter (ProjectEntityRest.addMagicFilterElements).
 *
 * Project favorites (`UserPrefArea.PROJEKT_FAVORITE`) are deliberately not carried over — the same
 * decision as the customer's; the list still offers the generic saved-filter favorites.
 */
export const PROJECT_PAGE = definePage<
  ProjectListRow,
  ProjectValues,
  ProjectDetail,
  typeof PROJEKT_METADATA
>({
  entity: "project",
  metadata: PROJEKT_METADATA,
  route: "/project",
  queryKey: PROJECT_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Projects (MenuCreator, PROJECT_LIST).
  categoryKey: "menu.fibu",
  titleKey: "fibu.projekt.title.list._",
  columns: [
    // The formatted number ("5.123.04"), read as one — the same value the legacy list leads with.
    // Keyed by the entity's computed `kost`, as the customer's number column is.
    {
      id: "kost",
      labelKey: "fibu.projekt.nummer",
      accessor: (row) => row.kostFormatted ?? "",
      size: 100,
      className: "font-mono",
    },
    { name: "identifier", size: 120 },
    // The customer, by name and division; sorted by the entity's own paths (`kunde`, not `customer`).
    {
      id: "kunde.name",
      labelKey: "fibu.kunde.name",
      accessor: (row) => row.customer?.name ?? "",
      referenceKey: "customer",
      size: 180,
    },
    {
      id: "kunde.division",
      labelKey: "fibu.kunde.division",
      accessor: (row) => row.customer?.division ?? "",
      referenceKey: "customer",
      size: 140,
    },
    { name: "name", size: 220, className: "font-semibold" },
    {
      name: "task",
      size: 180,
      // The plain task title with the path to the root as the tooltip, as the time sheet list shows it.
      cell: ({ row }) =>
        row.original.task?.title ?? row.original.task?.displayName ?? null,
      tooltip: (row) => row.task?.path ?? undefined,
    },
    // The enum cell renders the translated status label (ProjektStatus), so no separate statusAsString.
    { name: "status", size: 120 },
    {
      name: "headOfBusinessManager",
      size: 140,
      cell: ({ row }) =>
        row.original.headOfBusinessManager?.displayName ?? null,
    },
    {
      name: "salesManager",
      size: 140,
      cell: ({ row }) => row.original.salesManager?.displayName ?? null,
    },
    {
      name: "projectManager",
      size: 140,
      cell: ({ row }) => row.original.projectManager?.displayName ?? null,
    },
    {
      name: "projektManagerGroup",
      size: 160,
      cell: ({ row }) => row.original.projektManagerGroup?.displayName ?? null,
    },
    { name: "description", size: 300, wrap: true },
    // The two-digit ids of the project's cost 2 types, collected from the cost cache per row
    // (ProjectEntityRest.createListRow) — no property to order by.
    {
      id: "kost2ArtsAsString",
      labelKey: "fibu.kost2art.kost2arten",
      accessor: (row) => row.kost2ArtsAsString ?? "",
      size: 160,
      sortable: false,
      className: "font-mono text-muted-foreground",
    },
  ],
  // Mass update of a selection: the managers and the description (ProjectMultiSelectedPageRest,
  // /projectSelected).
  massUpdate: {
    endpoint: "projectSelected",
    route: "/project/mass-update",
  },
  edit: {
    schema: projectSchema,
    fieldNames: PROJECT_FIELDS,
    arrayFieldNames: ["kost2Arts"],
    defaultValues: emptyProjectValues,
    toFormValues,
    // The name is what identifies a project to a reader — the same string the list shows.
    title: (project) => project.name ?? "",
    newTitleKey: "fibu.projekt.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "fibu.projekt._",
        fields: [
          // The customer first: it decides the shape of the number beside it (ProjectNumberField).
          { custom: ProjectCustomerField, span: 2 },
          { custom: ProjectNumberField },
          { name: "name", span: 2 },
          { name: "identifier" },
          { name: "task", span: 2 },
          // The one value a reader looks for first — where the project stands in its lifecycle.
          { name: "status", emphasized: true },
          { custom: ProjectKontoField, span: 2 },
          { name: "projektManagerGroup" },
          { name: "projectManager" },
          { name: "headOfBusinessManager" },
          { name: "salesManager" },
          { name: "description", span: 3, rows: 4 },
          { custom: ProjectKost2TypesField, span: 3 },
        ],
      },
    ],
  },
});
