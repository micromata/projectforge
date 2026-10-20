"use client";

import { ExcelExportMenu } from "@/components/shared/export-menu";
import { downloadSkillMatrixExcel } from "@/lib/rs/skill-entry";

/**
 * The Excel export of the skill matrix, as the legacy React list offered it ("exportAsXls";
 * `SkillMatrixServicesRest`): the statistics over every skill, not the rows of the list, hence no filter.
 */
export function SkillEntryListActions() {
  return <ExcelExportMenu download={downloadSkillMatrixExcel} />;
}
