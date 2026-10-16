/**
 * The pure logic of the text parser of the address form — everything without DOM (see text-parser-model.test.ts):
 * which fields an offer holds, which of them start ticked, and where a ticked one lands in the form.
 *
 * An offer is what `address/parseText` (a pasted signature), `address/parseVcf` (a dropped vCard) or the entry of
 * a VCF import (`addressImport/entry`) propose: field name → value. The user ticks fields, may move a phone number
 * or an e-mail to another kind, and may move a whole address block (business, postal, private) to another one.
 */

import type { ParsedAddressField } from "@/lib/rs/address";

export type AddressBlock = "business" | "postal" | "private";

export const ADDRESS_BLOCKS: readonly AddressBlock[] = [
  "business",
  "postal",
  "private",
];

/** The fields of one address block, as the business block names them. */
const BLOCK_FIELDS = [
  "addressText",
  "addressText2",
  "zipCode",
  "city",
  "state",
  "country",
] as const;

export const PHONE_FIELDS = [
  "businessPhone",
  "mobilePhone",
  "fax",
  "privatePhone",
  "privateMobilePhone",
] as const;

export const EMAIL_FIELDS = ["email", "privateEmail"] as const;

/** Never ticked in advance while the form has one already: the text may be about somebody else. */
const NAME_FIELDS = ["name", "firstName"];

export type OfferedFields = Record<string, ParsedAddressField>;

/** The values of the form the offer is compared with, by field name. */
export type FormValues = Record<string, unknown>;

export function isPhoneField(field: string): boolean {
  return (PHONE_FIELDS as readonly string[]).includes(field);
}

export function isEmailField(field: string): boolean {
  return (EMAIL_FIELDS as readonly string[]).includes(field);
}

export function blockOf(field: string): AddressBlock | null {
  for (const block of ADDRESS_BLOCKS) {
    if (
      (BLOCK_FIELDS as readonly string[]).some(
        (base) => blockField(base, block) === field
      )
    ) {
      return block;
    }
  }
  return null;
}

/** The field of [block] for a base field: `city` → `postalCity`. */
export function blockField(base: string, block: AddressBlock): string {
  if (block === "business") return base;
  return `${block}${base.charAt(0).toUpperCase()}${base.slice(1)}`;
}

/** The base field of a block field: `postalCity` → `city`. */
export function baseFieldOf(field: string): string {
  for (const prefix of ["private", "postal"]) {
    if (field.startsWith(prefix) && blockOf(field) !== null) {
      const rest = field.slice(prefix.length);
      return rest.charAt(0).toLowerCase() + rest.slice(1);
    }
  }
  return field;
}

function normalized(value: unknown): string {
  return value == null ? "" : String(value).trim();
}

function hasValue(values: FormValues, field: string): boolean {
  return normalized(values[field]) !== "";
}

/**
 * The fields of a parsed text that would change the form: a value it already holds is no offer. A blank parsed
 * value is none either — a text that doesn't mention the fax says nothing about it.
 */
export function changedFields(
  offered: OfferedFields,
  values: FormValues
): OfferedFields {
  return Object.fromEntries(
    Object.entries(offered).filter(([field, offer]) => {
      const value = normalized(offer.value);
      return value !== "" && value !== normalized(values[field]);
    })
  );
}

/**
 * The fields of an import entry's vCard that differ from the form: changed, new, and removed ones — a vCard is
 * the whole contact, so a field it leaves blank is one it removes.
 */
export function fieldsFromComparison(
  comparison: Record<string, { vcf?: string | null; db?: string | null }>,
  values: FormValues
): OfferedFields {
  const result: OfferedFields = {};
  for (const [field, pair] of Object.entries(comparison)) {
    const vcf = normalized(pair.vcf);
    const current = normalized(values[field]);
    // The language is compared by its name (the form holds the locale, `de_DE`), so against the stored one.
    const unchanged =
      vcf === current ||
      (field === "communicationLanguage" && vcf === normalized(pair.db));
    if (!unchanged) {
      result[field] = { value: vcf, confidence: "high", currentValue: current };
    }
  }
  return result;
}

/** Every offered field starts ticked, except a name the form has one of already. */
export function initialSelection(
  offered: OfferedFields,
  values: FormValues
): Record<string, boolean> {
  return Object.fromEntries(
    Object.keys(offered).map((field) => [
      field,
      !(NAME_FIELDS.includes(field) && hasValue(values, field)),
    ])
  );
}

/** "Select all" spares a name the form has one of already, as the initial selection does. */
export function selectAll(
  offered: OfferedFields,
  values: FormValues,
  current: Record<string, boolean>
): Record<string, boolean> {
  return Object.fromEntries(
    Object.keys(offered).map((field) => [
      field,
      NAME_FIELDS.includes(field) && hasValue(values, field)
        ? current[field] === true
        : true,
    ])
  );
}

/** Where an offered field lands: its own field, unless the user moved it to another kind or block. */
export function targetOf(
  field: string,
  fieldMappings: Record<string, string>,
  blockMappings: Partial<Record<AddressBlock, AddressBlock>>
): string {
  const block = blockOf(field);
  if (block !== null) {
    return blockField(baseFieldOf(field), blockMappings[block] ?? block);
  }
  return fieldMappings[field] ?? field;
}

/** The ticked fields by the field they land in, with their values (blank for a field to be emptied). */
export function fieldsToApply(
  offered: OfferedFields,
  selection: Record<string, boolean>,
  fieldMappings: Record<string, string>,
  blockMappings: Partial<Record<AddressBlock, AddressBlock>>
): Record<string, string> {
  const result: Record<string, string> = {};
  for (const [field, offer] of Object.entries(offered)) {
    if (selection[field]) {
      result[targetOf(field, fieldMappings, blockMappings)] = offer.value ?? "";
    }
  }
  return result;
}
