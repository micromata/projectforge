import type {
  EntityMetadata,
  FieldMetadata,
  UIDataTypeName,
} from "@/lib/metadata/types";
import type { MerlinInputVariable, MerlinVariableType } from "@/lib/rs/merlin";

const DATA_TYPES: Record<MerlinVariableType, UIDataTypeName> = {
  STRING: "STRING",
  INT: "INT",
  FLOAT: "DECIMAL",
  DATE: "DATE",
};

/** The name of the form value of a variable, as the backend names it in its validation errors. */
export const inputFieldName = (variable: { name: string }) =>
  `inputVariables.${variable.name}`;

/**
 * What the fields of the execution form are validated and rendered against: the input variables of the
 * template (`inputVariables.<name>`) and the PDF option. Declared at runtime rather than generated because
 * the variables are the template's, not an entity's.
 */
export function merlinExecuteMetadata(
  variables: readonly MerlinInputVariable[]
): EntityMetadata {
  return {
    entity: "MerlinExecution",
    historizable: false,
    fields: Object.fromEntries<FieldMetadata>([
      ["pdfFormat", { dataType: "BOOLEAN", required: false }],
      ...variables.map((variable): [string, FieldMetadata] => [
        inputFieldName(variable),
        {
          dataType: variable.allowedValues?.length
            ? "STRING"
            : DATA_TYPES[variable.type],
          required: variable.required,
        },
      ]),
    ]),
  };
}
