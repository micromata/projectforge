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
 * translated result; export actions download a file; the mutating ones confirm first. "Dump database"
 * stays disabled (never migrated). The dev-only actions are appended only in development mode.
 */
export function SystemActionGroups({ data }: { data: SystemAdminData }) {
  const t = useTranslations();

  const development: SystemAction[] = [
    {
      key: "checkI18nProperties",
      labelKey: "system.admin.button.checkI18nProperties",
      tooltipKey: "system.admin.button.checkI18nProperties.tooltip",
      run: downloadCheckI18nProperties,
    },
    {
      key: "debugUserGroupCache",
      labelKey: "system.admin.button.debugUserGroupCache",
      tooltipKey: "system.admin.button.debugUserGroupCache.tooltip",
      run: downloadDebugUserGroupCache,
    },
  ];
  if (data.developmentMode) {
    development.push(
      {
        key: "testDatabase",
        labelKey: "system.admin.button.testDatabase",
        tooltipKey: "system.admin.button.testDatabase.tooltip",
        run: testDatabase,
      },
      {
        key: "createTestBooks",
        labelKey: "system.admin.button.createTestBooks",
        tooltipKey: "system.admin.button.createTestBooks.tooltip",
        confirmKey: "system.admin.development.testObjectsCreationQuestion",
        confirmValues: { "0": 100, "1": "BookDO" },
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
          labelKey: "system.admin.button.refreshCaches",
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
          labelKey: "system.admin.button.rereadConfiguration",
          tooltipKey: "system.admin.button.rereadConfiguration.tooltip",
          run: rereadConfiguration,
        },
        {
          key: "exportConfiguration",
          labelKey: "system.admin.button.exportConfiguration",
          tooltipKey: "system.admin.button.exportConfiguration.tooltip",
          run: downloadExportConfiguration,
        },
        {
          key: "export2FAConfiguration",
          labelKey: "system.admin.button.export2FAConfiguration",
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
          labelKey: "system.admin.button.checkSystemIntegrity",
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
          labelKey: "system.admin.button.createMissingDatabaseIndices",
          tooltipKey:
            "system.admin.button.createMissingDatabaseIndices.tooltip",
          run: createMissingIndices,
        },
        {
          key: "dump",
          labelKey: "system.admin.button.dump",
          disabled: true,
          disabledTooltipKey: "system.admin.button.dump.notMigrated",
        },
        {
          key: "exportSchema",
          labelKey: "system.admin.button.schemaExport",
          tooltipKey: "system.admin.button.schemaExport.tooltip",
          run: downloadExportSchema,
        },
        {
          key: "optimizeAddressImages",
          labelKey: "system.admin.button.optimizeAddressImages",
          tooltipKey: "system.admin.button.optimizeAddressImages.tooltip",
          confirmKey: "system.admin.button.optimizeAddressImages.question",
          run: downloadOptimizeAddressImages,
        },
        {
          key: "resetIdpPasswordSync",
          labelKey: "system.admin.button.resetIdpPasswordSync",
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
