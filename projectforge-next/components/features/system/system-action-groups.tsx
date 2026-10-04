"use client";

import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
  refreshCaches,
  rereadConfiguration,
  resetIdpPasswordSync,
  testDatabase,
} from "@/lib/rs/system";
import { SystemActionButton, type SystemAction } from "./system-action-button";
import type { SystemAdminData } from "./types";

interface ActionGroup {
  titleKey: string;
  actions: SystemAction[];
}

/**
 * All administration actions grouped as the classic page had them. Message actions toast their
 * translated result; export actions download a file; the mutating ones confirm first. The dev-only
 * actions are appended only in development mode.
 *
 * The label keys carry the trailing `._`: each has a `.tooltip` (or `.question`)
 * sibling, so the bundle nests the base value under `_` (see the generator's `._` convention, as in
 * `t("fibu.auftrag.position._")`). Without it `t()` would return the namespace object and show the key.
 */
export function SystemActionGroups({ data }: { data: SystemAdminData }) {
  const t = useTranslations();

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
  if (data.developmentMode) {
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

  const groups: ActionGroup[] = [
    {
      titleKey: "system.admin.group.title.systemChecksAndFunctionality.caches",
      actions: [
        {
          key: "refreshCaches",
          labelKey: "system.admin.button.refreshCaches._",
          tooltipKey: "system.admin.button.refreshCaches.tooltip",
          run: refreshCaches,
        },
      ],
    },
    {
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
    { titleKey: "system.admin.group.title.development", actions: development },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-2">
      {groups.map((group) => (
        <Card key={group.titleKey}>
          <CardHeader>
            <CardTitle>{t(group.titleKey)}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            {group.actions.map((action) => (
              <SystemActionButton key={action.key} action={action} />
            ))}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
