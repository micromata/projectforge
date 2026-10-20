import type { ContractEditValues } from "./contract-edit-schema";
import type { ContractDetail } from "../types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as `undefined`; every
 * value is normalised to null here, so no field ever holds `undefined`.
 */
export function toFormValues(contract: ContractDetail): ContractEditValues {
  return {
    id: contract.id ?? null,
    number: contract.number ?? null,
    title: contract.title ?? "",
    type: contract.type ?? null,
    status: contract.status ?? null,
    reference: contract.reference ?? null,
    date: contract.date ?? null,
    resubmissionOnDate: contract.resubmissionOnDate ?? null,
    dueDate: contract.dueDate ?? null,
    signingDate: contract.signingDate ?? null,
    validFrom: contract.validFrom ?? null,
    validUntil: contract.validUntil ?? null,
    coContractorA: contract.coContractorA ?? null,
    contractPersonA: contract.contractPersonA ?? null,
    signerA: contract.signerA ?? null,
    coContractorB: contract.coContractorB ?? null,
    contractPersonB: contract.contractPersonB ?? null,
    signerB: contract.signerB ?? null,
    text: contract.text ?? null,
    filing: contract.filing ?? null,
  };
}

/**
 * Blank form for a contract that doesn't exist yet. The backend presets today as its date
 * (`ContractEntityRest.newBaseDO`); its `/rs/contract/newEntry` answer is what the form is reset onto.
 */
export function emptyContractValues(): ContractEditValues {
  return toFormValues({ id: null });
}
