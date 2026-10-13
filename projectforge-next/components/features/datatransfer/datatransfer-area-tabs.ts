import type { EditPageTab } from "@/components/shared/edit-page-tabs";

/** The tabs of an area's page, by their id in the url (`?tab=info`); the files need none. */
export const DATA_TRANSFER_TAB = {
  files: "files",
  info: "info",
  audit: "audit",
  edit: "edit",
} as const;

export type DataTransferTab =
  (typeof DATA_TRANSFER_TAB)[keyof typeof DATA_TRANSFER_TAB];

/**
 * The open tab: the requested one if the user may see it, the files otherwise — an unknown `?tab=`, or
 * `?tab=edit` of a user who may not change the area (a forwarded link, a personal box).
 */
export function resolveDataTransferTab(
  requested: string | null,
  editAccess: boolean
): DataTransferTab {
  if (requested === DATA_TRANSFER_TAB.info) return DATA_TRANSFER_TAB.info;
  if (requested === DATA_TRANSFER_TAB.audit) return DATA_TRANSFER_TAB.audit;
  if (requested === DATA_TRANSFER_TAB.edit && editAccess) {
    return DATA_TRANSFER_TAB.edit;
  }
  return DATA_TRANSFER_TAB.files;
}

/** The tab bar: files, info, activities and — only for who may change the area — the admin form. */
export function dataTransferTabs(
  t: (key: string) => string,
  editAccess: boolean
): EditPageTab[] {
  const tabs: EditPageTab[] = [
    {
      id: DATA_TRANSFER_TAB.files,
      label: t("plugins.datatransfer.tab.files"),
      tab: DATA_TRANSFER_TAB.files,
    },
    {
      id: DATA_TRANSFER_TAB.info,
      label: t("plugins.datatransfer.tab.info"),
      tab: DATA_TRANSFER_TAB.info,
    },
    {
      id: DATA_TRANSFER_TAB.audit,
      label: t("plugins.datatransfer.audit._"),
      tab: DATA_TRANSFER_TAB.audit,
    },
  ];
  if (editAccess) {
    tabs.push({
      id: DATA_TRANSFER_TAB.edit,
      label: t("edit"),
      tab: DATA_TRANSFER_TAB.edit,
    });
  }
  return tabs;
}
