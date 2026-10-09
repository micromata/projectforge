import { SCRIPT_METADATA } from "@/lib/metadata/script.generated";
import { RichTextPreview } from "@/components/shared/rich-text-preview";
import { definePage } from "@/lib/page-def/define-page";
import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import {
  ScriptExecutableByGroupsField,
  ScriptExecutableByUsersField,
} from "./script-access-fields";
import { ScriptAvailableVariablesField } from "./script-available-variables";
import { ScriptCodeDownloads } from "./script-code-downloads";
import { ScriptCodeField } from "./script-code-field";
import { ScriptDescriptionField } from "./script-description-field";
import { ScriptFilenameField } from "./script-filename-field";
import { ScriptListActions } from "./script-list-actions";
import { ScriptParametersField } from "./script-parameters-field";
import { SCRIPT_ROUTE, scriptExecuteRoute } from "./script-routes";
import {
  scriptSchema,
  SCRIPT_FIELDS,
  type ScriptValues,
} from "./script-schema";
import { emptyScriptValues, toFormValues } from "./script-values";
import type { ScriptDetail, ScriptListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const SCRIPT_LIST_QUERY_KEY = ["script"] as const;

/** An include is only embedded into other scripts: no parameters, no access of its own, not executed. */
const isInclude = (data: Record<string, unknown> | undefined) =>
  data?.type === "INCLUDE";

/**
 * The scripts of the administration (financial and controlling staff) — list and form — as data (see
 * lib/page-def/types.ts).
 *
 * A row opens the script's *execution* (`/script/{id}`, the default of `openEntry`), which is what the
 * list is used for; the form is `/script/{id}/edit`, linked from there and beside the form's heading.
 */
export const SCRIPT_PAGE = definePage<
  ScriptListRow,
  ScriptValues,
  ScriptDetail,
  typeof SCRIPT_METADATA
>({
  entity: "script",
  metadata: SCRIPT_METADATA,
  route: SCRIPT_ROUTE,
  queryKey: SCRIPT_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Reporting (MenuItemDefId.SCRIPT_LIST).
  categoryKey: "menu.reporting",
  titleKey: "scripting.title.list",
  defaultSort: { id: "name" },
  columns: [
    { name: "name", size: 220, className: "font-semibold" },
    {
      name: "description",
      size: 300,
      // Rich text, or Markdown written before it was: as one plain line (see RichTextPreview).
      cell: (ctx) => (
        <RichTextPreview
          text={ctx.row.original.description}
          markdown
          highlight={ctx.table.options.meta?.highlight}
        />
      ),
    },
    {
      id: "parameterNames",
      labelKey: "scripting.script.parameters",
      accessor: (row) => row.parameterNames ?? "",
      sortable: false,
      size: 180,
    },
    { name: "type", size: 90 },
    { name: "executeAsUser", size: 140 },
    attachmentsColumn<ScriptListRow>(),
    {
      id: "executableByGroupsAsString",
      labelKey: "scripting.script.executableByGroups._",
      accessor: (row) => row.executableByGroupsAsString ?? "",
      sortable: false,
      size: 180,
    },
    {
      id: "executableByUsersAsString",
      labelKey: "scripting.script.executableByUsers._",
      accessor: (row) => row.executableByUsersAsString ?? "",
      sortable: false,
      size: 180,
    },
    { name: "lastUpdate", size: 130 },
    {
      id: "includes",
      labelKey: "scripting.script.includes",
      accessor: (row) => row.includes ?? "",
      sortable: false,
      size: 180,
    },
  ],
  listActions: ScriptListActions,
  edit: {
    schema: scriptSchema,
    fieldNames: SCRIPT_FIELDS,
    arrayFieldNames: ["executableByGroups", "executableByUsers"],
    defaultValues: emptyScriptValues,
    toFormValues,
    title: (script) => script.name ?? "",
    newTitleKey: "scripting.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    crossLinks: [
      {
        labelKey: "scripting.script.execute",
        href: (script) =>
          script.id != null && script.type !== "INCLUDE"
            ? scriptExecuteRoute(SCRIPT_ROUTE, script.id)
            : null,
        prominent: true,
      },
    ],
    // Beside the heading, where every edit page has its downloads (see InvoiceExportMenu).
    headerTrailing: (script) => <ScriptCodeDownloads id={script?.id} />,
    sections: [
      {
        id: "general",
        titleKey: "scripting.script._",
        fields: [
          { name: "name", span: 2 },
          { name: "type" },
          { custom: ScriptFilenameField },
          { custom: ScriptDescriptionField, span: 4 },
        ],
      },
      {
        id: "parameters",
        titleKey: "scripting.script.parameters",
        visible: ({ data }) => !isInclude(data),
        fields: [{ custom: ScriptParametersField, span: 4 }],
      },
      {
        id: "access",
        titleKey: "access.rights",
        visible: ({ data }) => !isInclude(data),
        fields: [
          { custom: ScriptExecutableByGroupsField, span: 2 },
          { custom: ScriptExecutableByUsersField, span: 2 },
          { name: "executeAsUser", span: 2 },
        ],
      },
      {
        id: "code",
        titleKey: "scripting.script.code",
        fields: [{ custom: ScriptCodeField, span: 4 }],
      },
      {
        id: "availableVariables",
        titleKey: "scripting.script.availableVariables",
        visible: ({ data }) => !!data?.availableVariables,
        fields: [{ custom: ScriptAvailableVariablesField, span: 4 }],
      },
      {
        id: "attachments",
        titleKey: "attachment.list",
        visible: ({ data }) => data?.id != null,
        render: ({ id }) => <AttachmentList entity="script" id={id} embedded />,
      },
    ],
  },
});
