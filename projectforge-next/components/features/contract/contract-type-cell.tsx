"use client";

import { useContractTypes } from "./edit/contract-type-field";

/** The label of a contract type in the list, or the stored value if the type is no longer configured. */
export function ContractTypeCell({ value }: { value?: string | null }) {
  const types = useContractTypes();
  if (!value) return null;
  return types.find((type) => type.value === value)?.label ?? value;
}
