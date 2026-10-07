import type { ComponentType } from "react";
import { CONTRIBUTION_MARGIN_PARAM } from "../contribution-margin-config";
import { LANES_AND_PLANES_PARAM } from "../lanes-and-planes-config";
import { ContributionMarginConfigEditor } from "./contribution-margin-config-editor";
import { LanesAndPlanesConfigEditor } from "./lanes-and-planes-config-editor";

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
};
