"use client";

import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportMenu } from "@/components/shared/export-menu";
import { ProjectWizardLink } from "@/components/shared/tasks/project-wizard-link";

/**
 * The actions of the project list: the Excel export of the projects the filter matches, with all their
 * fields (`ProjectEntityRest.exportAsExcel`), and the project wizard, which sets up a new project together
 * with its customer, structure element, groups and access rights — hidden for users who may not run it.
 *
 * The export acts on the filter the list is showing (see PageDef.listActions). Select access is all its
 * endpoint asks for, so there is no gate.
 */
export function ProjectListActions({ filter }: { filter: MagicFilter }) {
  return (
    <>
      <ExcelExportMenu download={() => downloadListExcel("project", filter)} />
      <ProjectWizardLink />
    </>
  );
}
