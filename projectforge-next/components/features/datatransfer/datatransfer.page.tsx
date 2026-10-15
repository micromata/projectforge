import { DATA_TRANSFER_AREA_METADATA } from "@/lib/metadata/data-transfer-area.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  DataTransferAccessGroupsField,
  DataTransferAccessUsersField,
  DataTransferAdminsField,
  DataTransferObserversField,
} from "./datatransfer-access-fields";
import { DataTransferCapacityBar } from "./datatransfer-capacity-bar";
import { DataTransferExternalAccess } from "./datatransfer-external-access";
import { DataTransferListActions } from "./datatransfer-list-actions";
import {
  dataTransferSchema,
  DATA_TRANSFER_FIELDS,
  type DataTransferValues,
} from "./datatransfer-schema";
import {
  DataTransferExpiryDaysField,
  DataTransferMaxUploadSizeField,
} from "./datatransfer-select-fields";
import { emptyDataTransferValues, toFormValues } from "./datatransfer-values";
import type { DataTransferAreaDetail, DataTransferListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const DATA_TRANSFER_LIST_QUERY_KEY = ["datatransfer"] as const;

/**
 * The data transfer areas — list and admin form — as data (see lib/page-def/types.ts).
 *
 * A row opens the area's *files* (`/datatransfer/{id}`, the default of `openEntry`), not the form: the
 * files are what every reader of the list is after, and only the area's admins may change it. The form
 * is the area page's edit tab (`/datatransfer/{id}?tab=edit`).
 *
 * The columns are the legacy list's, in its order. Everything that holds users or groups is the DTO's
 * string of names, which the backend cannot sort by (they are csv lists of ids).
 */
export const DATA_TRANSFER_PAGE = definePage<
  DataTransferListRow,
  DataTransferValues,
  DataTransferAreaDetail,
  typeof DATA_TRANSFER_AREA_METADATA
>({
  entity: "datatransfer",
  metadata: DATA_TRANSFER_AREA_METADATA,
  route: "/datatransfer",
  queryKey: DATA_TRANSFER_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (DataTransferPlugin, MenuItemDefId.MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.datatransfer.title.list",
  // The latest change first.
  defaultSort: { id: "lastUpdate", desc: true },
  columns: [
    { name: "created", size: 130 },
    {
      // Sorted by the timestamp it is computed from; "3 hours ago" is the backend's, in the user's locale.
      id: "lastUpdate",
      labelKey: "modified",
      accessor: (row) => row.lastUpdateTimeAgo ?? "",
      size: 130,
    },
    { name: "areaName", size: 200, className: "font-semibold" },
    { name: "description", size: 260 },
    {
      id: "attachmentsSizeFormatted",
      labelKey: "attachments._",
      accessor: (row) => row.attachmentsSizeFormatted ?? "",
      sortable: false,
      size: 100,
      align: "right",
    },
    {
      // How full the area is, as a bar; the numbers ("11.7 MB/20 GB (0%)") on hover.
      id: "capacity",
      labelKey: "plugins.datatransfer.capacity._",
      accessor: (row) => row.capacity?.capacityAsMessage ?? "",
      sortable: false,
      filterKind: null,
      size: 110,
      tooltip: (row) => row.capacity?.capacityAsMessage ?? undefined,
      cell: (ctx) => (
        <DataTransferCapacityBar
          capacity={ctx.row.original.capacity}
          label={ctx.row.original.capacity?.capacityAsMessage ?? ""}
        />
      ),
    },
    {
      id: "capacity.maxUploadSizeFormatted",
      labelKey: "plugins.datatransfer.maxUploadSize._",
      accessor: (row) => row.capacity?.maxUploadSizeFormatted ?? "",
      sortable: false,
      size: 110,
      align: "right",
    },
    {
      name: "externalDownloadEnabled",
      labelKey: "plugins.datatransfer.external.download.enabled.title",
      size: 90,
    },
    {
      name: "externalUploadEnabled",
      labelKey: "plugins.datatransfer.external.upload.enabled.title",
      size: 90,
    },
    { name: "expiryDays", size: 90 },
    ...(
      [
        ["adminsAsString", "plugins.datatransfer.admins._"],
        ["observersAsString", "plugins.datatransfer.observers._"],
        ["accessUsersAsString", "plugins.datatransfer.accessUsers._"],
        ["accessGroupsAsString", "plugins.datatransfer.accessGroups._"],
      ] as const
    ).map(([id, labelKey]) => ({
      id,
      labelKey,
      accessor: (row: DataTransferListRow) => row[id] ?? "",
      sortable: false as const,
      size: 180,
    })),
  ],
  listActions: DataTransferListActions,
  edit: {
    schema: dataTransferSchema,
    fieldNames: DATA_TRANSFER_FIELDS,
    arrayFieldNames: ["admins", "observers", "accessUsers", "accessGroups"],
    defaultValues: emptyDataTransferValues,
    toFormValues,
    title: (area) => area.areaName ?? "",
    newTitleKey: "plugins.datatransfer.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "plugins.datatransfer.title.heading",
        fields: [
          { name: "areaName", span: 2 },
          { custom: DataTransferExpiryDaysField },
          { custom: DataTransferObserversField, span: 2 },
          { custom: DataTransferMaxUploadSizeField },
          {
            name: "description",
            span: 3,
            rows: 3,
            hintKey: "plugins.datatransfer.description.info",
          },
        ],
      },
      {
        id: "access",
        titleKey: "plugins.datatransfer.access.title",
        fields: [
          { custom: DataTransferAdminsField, span: 3 },
          { custom: DataTransferAccessUsersField, span: 3 },
          { custom: DataTransferAccessGroupsField, span: 3 },
        ],
      },
      {
        // A personal box is its owner's alone: no external access (DataTransferAreaDao.ensurePersonalBox).
        id: "external",
        titleKey: "plugins.datatransfer.external.access.title",
        visible: ({ data }) => data?.personalBox !== true,
        fields: [{ custom: DataTransferExternalAccess, span: 3 }],
      },
    ],
  },
});
