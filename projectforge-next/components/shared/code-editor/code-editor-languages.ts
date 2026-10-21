import { StreamLanguage } from "@codemirror/language";
import { tags } from "@lezer/highlight";
import { kotlin } from "@codemirror/legacy-modes/mode/clike";
import { groovy } from "@codemirror/legacy-modes/mode/groovy";
import { json } from "@codemirror/legacy-modes/mode/javascript";
import { properties } from "@codemirror/legacy-modes/mode/properties";

/** The syntaxes the code editor and its diff highlight (see CodeEditorProps.language). */
export const CODE_EDITOR_LANGUAGES = {
  kotlin: StreamLanguage.define(kotlin),
  groovy: StreamLanguage.define(groovy),
  properties: StreamLanguage.define(properties),
  // The mode names the keys "property", which the default token table doesn't know.
  json: StreamLanguage.define({
    ...json,
    tokenTable: { property: tags.propertyName },
  }),
};
