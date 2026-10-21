import type { MerlinVariableType } from "@/lib/rs/merlin";

/** The labels of the variable types, spelled out for the i18n scan. */
export const MERLIN_VARIABLE_TYPE_KEYS: Record<MerlinVariableType, string> = {
  STRING: "plugins.merlin.variable.type.STRING",
  INT: "plugins.merlin.variable.type.INT",
  FLOAT: "plugins.merlin.variable.type.FLOAT",
  DATE: "plugins.merlin.variable.type.DATE",
};
