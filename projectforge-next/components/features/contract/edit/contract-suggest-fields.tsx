"use client";

import type { ComponentType } from "react";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { CONTRACT_METADATA } from "@/lib/metadata/contract.generated";
import {
  fetchContractSuggestions,
  type ContractSuggestedProperty,
} from "@/lib/rs/contract";

/**
 * A free text completing from the values other contracts already have, as the autocompletion of the former React
 * form (`ContractDao.isAutocompletionPropertyEnabled` opts the properties in).
 *
 * A factory, as a section's `custom` slot renders a bare `ComponentType<{ className?: string }>`; called once at
 * module load, so every field is a stable component.
 */
function makeContractSuggestField(
  property: ContractSuggestedProperty
): ComponentType<{ className?: string }> {
  function ContractSuggestField({ className }: { className?: string }) {
    const label = useFieldLabels(CONTRACT_METADATA);
    return (
      <StringSuggestField
        name={property}
        label={label(property)}
        className={className}
        suggest={(search, signal) =>
          fetchContractSuggestions(property, search, signal)
        }
        // The completions depend on nothing but the property and the term the user is typing.
        queryKey={["contract", "autocomplete", property]}
      />
    );
  }
  ContractSuggestField.displayName = `ContractSuggestField(${property})`;
  return ContractSuggestField;
}

export const ContractTitleField = makeContractSuggestField("title");
export const CoContractorAField = makeContractSuggestField("coContractorA");
export const ContractPersonAField = makeContractSuggestField("contractPersonA");
export const SignerAField = makeContractSuggestField("signerA");
export const CoContractorBField = makeContractSuggestField("coContractorB");
export const ContractPersonBField = makeContractSuggestField("contractPersonB");
export const SignerBField = makeContractSuggestField("signerB");
