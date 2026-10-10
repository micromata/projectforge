import { toRichTextHtml } from "@/components/shared/rich-text";
import type { ScriptParam, ScriptParameterKey } from "@/lib/rs/script";
import type { ScriptValues } from "./script-schema";
import type { ScriptDetail } from "./types";

function parameter(param: ScriptParam | null | undefined) {
  return {
    name: param?.name ?? null,
    type: param?.type ?? null,
    description: richText(param?.description),
  };
}

/** A description written in Markdown before it was rich text becomes the editor's HTML. */
const richText = (text: string | null | undefined) =>
  text ? toRichTextHtml(text) : null;

const param = (script: ScriptDetail, key: ScriptParameterKey) =>
  parameter(script[key]);

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(script: ScriptDetail): ScriptValues {
  return {
    id: script.id ?? null,
    name: script.name ?? "",
    type: script.type ?? null,
    description: richText(script.description),
    script: script.script ?? null,
    parameter1: param(script, "parameter1"),
    parameter2: param(script, "parameter2"),
    parameter3: param(script, "parameter3"),
    parameter4: param(script, "parameter4"),
    parameter5: param(script, "parameter5"),
    parameter6: param(script, "parameter6"),
    executableByGroups: script.executableByGroups ?? [],
    executableByUsers: script.executableByUsers ?? [],
    executeAsUser: script.executeAsUser ?? null,
    pageTargetIds: script.pageTargetIds ?? [],
    buttonLabel: script.buttonLabel ?? null,
    buttonTooltip: script.buttonTooltip ?? null,
    created: script.created ?? null,
  };
}

/**
 * Blank form for a script that doesn't exist yet. The backend presets the type (KOTLIN,
 * `ScriptEntityRest.newBaseDO`), and its `/rs/script/newEntry` answer is what the form is reset onto.
 */
export function emptyScriptValues(): ScriptValues {
  return toFormValues({ id: null, type: "KOTLIN" });
}
