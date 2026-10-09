import type {
  EntityMetadata,
  FieldMetadata,
  UIDataTypeName,
} from "@/lib/metadata/types";
import { SCRIPT_PARAMETER_KEYS } from "@/lib/rs/script";

/** The value properties of `Script.Param`, by the type the shared field components render them as. */
const VALUE_TYPES: Record<string, UIDataTypeName> = {
  stringValue: "STRING",
  intValue: "INT",
  decimalValue: "DECIMAL",
  booleanValue: "BOOLEAN",
  dateValue: "DATE",
  toDateValue: "DATE",
  userValue: "USER",
  taskValue: "TASK",
};

/**
 * What the shared fields of the execution form are validated and rendered against: the parameter
 * values of the DTO (`parameter1.intValue` & co.), none mandatory — a script decides itself what it
 * makes of a missing value — plus the code of an ad-hoc execution.
 *
 * Declared here rather than generated because no entity has these fields: they are the DTO's.
 */
export const SCRIPT_EXECUTE_METADATA: EntityMetadata = {
  entity: "ScriptExecution",
  historizable: false,
  fields: Object.fromEntries<FieldMetadata>([
    ["script", { dataType: "STRING", required: false }],
    ...SCRIPT_PARAMETER_KEYS.flatMap((key) =>
      Object.entries(VALUE_TYPES).map(
        ([property, dataType]): [string, FieldMetadata] => [
          `${key}.${property}`,
          { dataType, required: false },
        ]
      )
    ),
  ]),
};
