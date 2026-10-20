import { CONTRACT_METADATA } from "@/lib/metadata/contract.generated";
import { definePage } from "@/lib/page-def/define-page";
import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { makeJiraFieldLinks } from "@/components/shared/jira/jira-field-links";
import { ContractTypeCell } from "./contract-type-cell";
import {
  contractEditSchema,
  CONTRACT_EDIT_FIELDS,
  type ContractEditValues,
} from "./edit/contract-edit-schema";
import { emptyContractValues, toFormValues } from "./edit/contract-edit-values";
import {
  CoContractorAField,
  CoContractorBField,
  ContractPersonAField,
  ContractPersonBField,
  ContractTitleField,
  SignerAField,
  SignerBField,
} from "./edit/contract-suggest-fields";
import { ContractTypeField } from "./edit/contract-type-field";
import type { ContractDetail, ContractListRow } from "./types";

/** REST category of a contract — the entity name every shared hook is parameterised with. */
export const CONTRACT_ENTITY = "contract";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const CONTRACT_LIST_QUERY_KEY = ["contract"] as const;

// Created once at module load, so the component is a stable reference (see makeJiraFieldLinks).
const TextJiraLinks = makeJiraFieldLinks("text");

/** A contract is referred to by its number with the prefix of the former pages, `C-1042`. */
function formatNumber(contractNumber?: number | null): string | null {
  return contractNumber != null ? `C-${contractNumber}` : null;
}

/**
 * The contracts — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `ContractPagesRest`, which it follows: the list's columns in their order, and
 * the form's blocks — the contract itself, its dates, both contracting parties, the text with its JIRA issues, the
 * filing and the attachments. The year filter of the React list is the period filter of the date column here.
 */
export const CONTRACT_PAGE = definePage<
  ContractListRow,
  ContractEditValues,
  ContractDetail,
  typeof CONTRACT_METADATA
>({
  entity: CONTRACT_ENTITY,
  metadata: CONTRACT_METADATA,
  route: "/contract",
  queryKey: CONTRACT_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Organization > Contracts (MenuCreator, MenuItemDefId.CONTRACTS).
  categoryKey: "menu.orga",
  titleKey: "legalAffaires.contract.title.list",
  // Newest first: the latest contracts are the ones looked for.
  defaultSort: { id: "number", desc: true },
  columns: [
    {
      name: "number",
      size: 110,
      className: "font-medium",
      cell: ({ row }) => formatNumber(row.original.number),
    },
    { name: "date", size: 110 },
    {
      name: "type",
      size: 140,
      cell: ({ row }) => <ContractTypeCell value={row.original.type} />,
    },
    { name: "status", size: 130 },
    attachmentsColumn<ContractListRow>(),
    // No link in the cell: the whole row navigates to the edit page.
    { name: "title", size: 280, minSize: 200, className: "font-semibold" },
    { name: "coContractorA", size: 180 },
    { name: "coContractorB", size: 180 },
    { name: "resubmissionOnDate", size: 110 },
    { name: "dueDate", size: 110 },
  ],
  // A contract is looked for by the year it was dated in, as the year filter of the React list offered it.
  filterPeriodKinds: ["month", "year"],
  filterDefaultPeriodKind: "year",
  edit: {
    schema: contractEditSchema,
    fieldNames: CONTRACT_EDIT_FIELDS,
    defaultValues: emptyContractValues,
    toFormValues,
    title: (contract) =>
      [formatNumber(contract.number), contract.title].filter(Boolean).join(" "),
    newTitleKey: "legalAffaires.contract.title.add",
    savedMessageKey: "message.successfullChanged",
    // A follow-up contract is written by cloning an existing one; the copy gets a number of its own on saving
    // (ContractEntityRest.prepareClone).
    clone: true,
    sections: [
      {
        id: "general",
        titleKey: "legalAffaires.contract.sections.general",
        highlighted: true,
        fields: [
          // Assigned by the backend on the first save (ContractDao), and never changed afterwards.
          { name: "number", readOnly: true, maxDigits: 6 },
          { custom: ContractTitleField, span: 2 },
          { custom: ContractTypeField },
          { name: "status", emphasized: true },
          { name: "reference", jiraHint: true },
          { name: "date", startsRow: true },
          { name: "resubmissionOnDate" },
          { name: "dueDate" },
          { name: "signingDate" },
          {
            // The bare key of a namespace with children, hence `._`.
            periodLabelKey: "legalAffaires.contract.validity._",
            begin: "validFrom",
            end: "validUntil",
            span: 2,
          },
        ],
      },
      {
        id: "contractorA",
        titleKey: "legalAffaires.contract.coContractorA",
        fields: [
          { custom: CoContractorAField, span: 3 },
          { custom: ContractPersonAField },
          { custom: SignerAField },
        ],
      },
      {
        id: "contractorB",
        titleKey: "legalAffaires.contract.coContractorB",
        fields: [
          { custom: CoContractorBField, span: 3 },
          { custom: ContractPersonBField },
          { custom: SignerBField },
        ],
      },
      {
        id: "text",
        titleKey: "text",
        fields: [
          { name: "text", rows: 5, span: 3, jiraHint: true },
          { custom: TextJiraLinks, span: 3 },
          { name: "filing", rows: 2, span: 3 },
        ],
      },
      {
        id: "attachments",
        // The title of the attachment fieldset of the React form, reused rather than written again.
        titleKey: "attachment.list",
        render: ({ id }) => (
          <AttachmentList entity={CONTRACT_ENTITY} id={id} embedded />
        ),
      },
    ],
  },
});
