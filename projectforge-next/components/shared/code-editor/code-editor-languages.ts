import { StreamLanguage } from "@codemirror/language";
import { kotlin } from "@codemirror/legacy-modes/mode/clike";
import { groovy } from "@codemirror/legacy-modes/mode/groovy";

/** The syntaxes the code editor and its diff highlight (see CodeEditorProps.language). */
export const CODE_EDITOR_LANGUAGES = {
  kotlin: StreamLanguage.define(kotlin),
  groovy: StreamLanguage.define(groovy),
};
