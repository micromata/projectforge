import {
  createMissingIndices,
  createTestBooks,
  downloadCheckI18nProperties,
  downloadCheckSystemIntegrity,
  downloadDebugUserGroupCache,
  downloadExport2FAConfiguration,
  downloadExportConfiguration,
  downloadExportSchema,
  downloadOptimizeAddressImages,
  rereadConfiguration,
  resetIdpPasswordSync,
  testDatabase,
  type SystemMessageResponse,
} from "@/lib/rs/system";

/** One administration action, an item of the System page's menu bar (see [SystemMenuBar]). */
export interface SystemAction {
  /** Stable key for React and for finding the action in tests. */
  key: string;
  /** i18n key of the item label. */
  labelKey: string;
  /** i18n key of the explanation shown below the label, if any. */
  tooltipKey?: string;
  /** What the item does: a message action ({ message }) or a file download (void). */
  run: () => Promise<SystemMessageResponse | void>;
  /** i18n key of a yes/no question asked before [run] fires. */
  confirmKey?: string;
  /** ICU values for [confirmKey] (the backend `{0}`/`{1}` placeholders become named args `arg0`/`arg1`). */
  confirmValues?: Record<string, string | number>;
}

/** A top-level menu of the menu bar. */
export interface SystemActionMenu {
  id: "configuration" | "checks" | "database" | "development";
  titleKey: string;
  actions: SystemAction[];
}

/**
 * All one-click administration actions, grouped into the menus of the menu bar as the classic page
 * grouped them. The dev-only actions are appended only in development mode.
 *
 * The label keys carry the trailing `._`: each has a `.tooltip` (or `.question`)
 * sibling, so the bundle nests the base value under `_` (see the generator's `._` convention, as in
 * `t("fibu.auftrag.position._")`). Without it `t()` would return the namespace object and show the key.
 */
export function systemActionMenus(
  developmentMode: boolean
): SystemActionMenu[] {
  const development: SystemAction[] = [
    {
      key: "checkI18nProperties",
      labelKey: "system.admin.button.checkI18nProperties._",
      tooltipKey: "system.admin.button.checkI18nProperties.tooltip",
      run: downloadCheckI18nProperties,
    },
    {
      key: "debugUserGroupCache",
      labelKey: "system.admin.button.debugUserGroupCache._",
      tooltipKey: "system.admin.button.debugUserGroupCache.tooltip",
      run: downloadDebugUserGroupCache,
    },
  ];
  if (developmentMode) {
    development.push(
      {
        key: "testDatabase",
        labelKey: "system.admin.button.testDatabase._",
        tooltipKey: "system.admin.button.testDatabase.tooltip",
        run: testDatabase,
      },
      {
        key: "createTestBooks",
        labelKey: "system.admin.button.createTestBooks._",
        tooltipKey: "system.admin.button.createTestBooks.tooltip",
        confirmKey: "system.admin.development.testObjectsCreationQuestion",
        confirmValues: { arg0: 100, arg1: "BookDO" },
        run: createTestBooks,
      }
    );
  }

  return [
    {
      id: "configuration",
      titleKey:
        "system.admin.group.title.systemChecksAndFunctionality.configuration",
      actions: [
        {
          key: "rereadConfiguration",
          labelKey: "system.admin.button.rereadConfiguration._",
          tooltipKey: "system.admin.button.rereadConfiguration.tooltip",
          run: rereadConfiguration,
        },
        {
          key: "exportConfiguration",
          labelKey: "system.admin.button.exportConfiguration._",
          tooltipKey: "system.admin.button.exportConfiguration.tooltip",
          run: downloadExportConfiguration,
        },
        {
          key: "export2FAConfiguration",
          labelKey: "system.admin.button.export2FAConfiguration._",
          tooltipKey: "system.admin.button.export2FAConfiguration.tooltip",
          run: downloadExport2FAConfiguration,
        },
      ],
    },
    {
      id: "checks",
      titleKey: "system.admin.group.title.checks",
      actions: [
        {
          key: "checkSystemIntegrity",
          labelKey: "system.admin.button.checkSystemIntegrity._",
          tooltipKey: "system.admin.button.checkSystemIntegrity.tooltip",
          run: downloadCheckSystemIntegrity,
        },
      ],
    },
    {
      id: "database",
      titleKey: "system.admin.group.title.databaseActions",
      actions: [
        {
          key: "createMissingIndices",
          labelKey: "system.admin.button.createMissingDatabaseIndices._",
          tooltipKey:
            "system.admin.button.createMissingDatabaseIndices.tooltip",
          run: createMissingIndices,
        },
        {
          key: "exportSchema",
          labelKey: "system.admin.button.schemaExport._",
          tooltipKey: "system.admin.button.schemaExport.tooltip",
          run: downloadExportSchema,
        },
        {
          key: "optimizeAddressImages",
          labelKey: "system.admin.button.optimizeAddressImages._",
          tooltipKey: "system.admin.button.optimizeAddressImages.tooltip",
          confirmKey: "system.admin.button.optimizeAddressImages.question",
          run: downloadOptimizeAddressImages,
        },
        {
          key: "resetIdpPasswordSync",
          labelKey: "system.admin.button.resetIdpPasswordSync._",
          tooltipKey: "system.admin.button.resetIdpPasswordSync.tooltip",
          confirmKey: "system.admin.button.resetIdpPasswordSync.question",
          run: resetIdpPasswordSync,
        },
      ],
    },
    {
      id: "development",
      titleKey: "system.admin.group.title.development",
      actions: development,
    },
  ];
}
