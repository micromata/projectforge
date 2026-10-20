// Mirrors org.projectforge.rest.dto.Contract (projectforge-rest). Keep field names in sync with the Spring DTO.
import type { CONTRACT_METADATA } from "@/lib/metadata/contract.generated";

/** The constants of org.projectforge.business.orga.ContractStatus, taken from the generated metadata. */
export type ContractStatus =
  (typeof CONTRACT_METADATA.fields.status.enumValues)[number]["value"];

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
 * (JacksonConfiguration), so an empty field is absent from the JSON rather than null. `toFormValues` normalises
 * that away.
 */
export interface ContractDetail {
  /** null for a contract that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /** Assigned by the backend on the first save (`ContractDao`), shown as `C-<number>`. */
  number?: number | null;
  date?: string | null;
  validFrom?: string | null;
  validUntil?: string | null;
  title?: string | null;
  coContractorA?: string | null;
  contractPersonA?: string | null;
  signerA?: string | null;
  coContractorB?: string | null;
  contractPersonB?: string | null;
  signerB?: string | null;
  signingDate?: string | null;
  /** The value of a configured contract type (configuration parameter `orga.contractTypes`). */
  type?: string | null;
  status?: ContractStatus | null;
  text?: string | null;
  reference?: string | null;
  filing?: string | null;
  resubmissionOnDate?: string | null;
  dueDate?: string | null;
}

/** Projection used by the list page. */
export interface ContractListRow {
  id: number;
  number?: number | null;
  date?: string | null;
  type?: string | null;
  status?: ContractStatus | null;
  title?: string | null;
  coContractorA?: string | null;
  coContractorB?: string | null;
  resubmissionOnDate?: string | null;
  dueDate?: string | null;
  /** Number of attachments; absent for a contract that never had one. */
  attachmentsCounter?: number | null;
  /** Their total size in bytes — sorted on, not displayed (see attachmentsSizeFormatted). */
  attachmentsSize?: number | null;
  /** Size and count in one, formatted by the backend in the user's locale ("5,2MB (3)"), or "-". */
  attachmentsSizeFormatted?: string | null;
}
