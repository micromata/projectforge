import type { ComponentType } from "react";
import { CONTRIBUTION_MARGIN_PARAM } from "../contribution-margin-config";
import { LANES_AND_PLANES_PARAM } from "../lanes-and-planes-config";
import { ContributionMarginConfigEditor } from "./contribution-margin-config-editor";
import { LanesAndPlanesConfigEditor } from "./lanes-and-planes-config-editor";
import { FORECAST_PARAM } from "../forecast-config";
import { ForecastConfigEditor } from "./forecast-config-editor";
import { GROUP_NOMENCLATURE_PARAM } from "@/lib/group-nomenclature";
import { GroupNomenclatureConfigEditor } from "./group-nomenclature-config-editor";
import { HOLIDAYS_PARAM } from "../holidays-config";
import { HolidaysConfigEditor } from "./holidays-config-editor";
import { ACCOUNTING_PARAM } from "../accounting-config";
import { AccountingConfigEditor } from "./accounting-config-editor";
import { CONTRACT_TYPES_PARAM } from "../contract-types-config";
import { ContractTypesConfigEditor } from "./contract-types-config-editor";

/** What the editor of a JSON parameter gets; it binds to the form value `stringValue` itself. */
export interface JsonEditorProps {
  label: string;
  hint?: string;
  className?: string;
}

/**
 * The editors of the JSON parameters (`ConfigurationType.JSON`), by parameter key. A JSON parameter
 * without one is edited as plain text (see ConfigurationValueField).
 */
export const JSON_EDITORS: Record<string, ComponentType<JsonEditorProps>> = {
  [CONTRIBUTION_MARGIN_PARAM]: ContributionMarginConfigEditor,
  [LANES_AND_PLANES_PARAM]: LanesAndPlanesConfigEditor,
  [FORECAST_PARAM]: ForecastConfigEditor,
  [GROUP_NOMENCLATURE_PARAM]: GroupNomenclatureConfigEditor,
  [HOLIDAYS_PARAM]: HolidaysConfigEditor,
  [ACCOUNTING_PARAM]: AccountingConfigEditor,
  [CONTRACT_TYPES_PARAM]: ContractTypesConfigEditor,
};
