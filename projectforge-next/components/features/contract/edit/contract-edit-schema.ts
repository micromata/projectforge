import { z } from "zod";
import { CONTRACT_METADATA } from "@/lib/metadata/contract.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length, the constants of the status — comes from ContractDO through
 * `lib/metadata/contract.generated.ts` (see lib/validation/from-metadata.ts).
 *
 * The server validates too and has the last word (ContractEntityRest.validate: no date in the future, HTTP 406 →
 * see lib/validation/server-errors.ts).
 */
const m = fromMetadata(CONTRACT_METADATA);

export const contractEditSchema = z.object({
  // null while the contract is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  // Assigned by the backend on the first save, read-only in the form.
  number: m.intField("number"),
  title: m.requiredString("title"),
  type: m.nullableString("type"),
  status: m.enumField("status"),
  reference: m.nullableString("reference"),
  date: m.instantField("date"),
  resubmissionOnDate: m.instantField("resubmissionOnDate"),
  dueDate: m.instantField("dueDate"),
  signingDate: m.instantField("signingDate"),
  validFrom: m.instantField("validFrom"),
  validUntil: m.instantField("validUntil"),
  coContractorA: m.nullableString("coContractorA"),
  contractPersonA: m.nullableString("contractPersonA"),
  signerA: m.nullableString("signerA"),
  coContractorB: m.nullableString("coContractorB"),
  contractPersonB: m.nullableString("contractPersonB"),
  signerB: m.nullableString("signerB"),
  text: m.nullableString("text"),
  filing: m.nullableString("filing"),
});

export type ContractEditValues = z.infer<typeof contractEditSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders (see
 * applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const CONTRACT_EDIT_FIELDS = Object.keys(
  contractEditSchema.shape
) as readonly (keyof ContractEditValues)[];
