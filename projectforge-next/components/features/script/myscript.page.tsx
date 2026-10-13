import { SCRIPT_METADATA } from "@/lib/metadata/script.generated";
import { RichTextPreview } from "@/components/shared/rich-text-preview";
import { defineListPage } from "@/lib/page-def/define-page";
import { MY_SCRIPT_ROUTE } from "./script-routes";
import type { ScriptListRow } from "./types";

/**
 * The scripts the logged-in user is allowed to execute (`MyScriptEntityRest`) — a list only: a row opens
 * the execution (`/myscript/{id}`), and the code and the access of a script stay the administration's.
 * `foreignEdit` because that page is this app's, though no form of the entity.
 */
export const MY_SCRIPT_PAGE = defineListPage<
  ScriptListRow,
  typeof SCRIPT_METADATA
>({
  entity: "myscript",
  metadata: SCRIPT_METADATA,
  route: MY_SCRIPT_ROUTE,
  queryKey: ["myscript"],
  // Where the entry sits in the main menu: Project management (MenuItemDefId.MY_SCRIPT_LIST).
  categoryKey: "menu.projectmanagement",
  titleKey: "scripting.myScript.list",
  defaultSort: { id: "name" },
  foreignEdit: true,
  onRowClick: (row) => `${MY_SCRIPT_ROUTE}/${row.id}`,
  columns: [
    { name: "name", size: 220, className: "font-semibold" },
    {
      name: "description",
      size: 360,
      // Rich text, or Markdown written before it was: as one plain line (see RichTextPreview).
      cell: (ctx) => (
        <RichTextPreview
          text={ctx.row.original.description}
          highlight={ctx.table.options.meta?.highlight}
        />
      ),
    },
    {
      id: "parameterNames",
      labelKey: "scripting.script.parameters",
      accessor: (row) => row.parameterNames ?? "",
      sortable: false,
      size: 200,
    },
    { name: "type", size: 90 },
    { name: "lastUpdate", size: 130 },
  ],
});
