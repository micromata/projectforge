import type { PropertyParam } from "@/lib/rs/properties-editor";

/** A key of an indexed list: the list before its last index, the index, the field after it. */
export interface ListEntryKey {
  list: string;
  index: number;
  field: string;
}

const LIST_ENTRY_PATTERN = /^(.*)\[(\d+)\]([^[\]]*)$/;

/** `a.bankAccounts[2].iban` → list `a.bankAccounts`, index 2, field `.iban`; null for other keys. */
export function parseListEntryKey(key: string): ListEntryKey | null {
  const match = LIST_ENTRY_PATTERN.exec(key);
  return match
    ? { list: match[1], index: Number(match[2]), field: match[3] }
    : null;
}

/** The lists of the given parameters in the order of their first appearance. */
export function listsOf(params: PropertyParam[]): string[] {
  const lists = params
    .map((param) => parseListEntryKey(param.key)?.list)
    .filter((list): list is string => !!list);
  return [...new Set(lists)];
}

/** The short name of a list for its button, e.g. `projectforge.einvoice.seller.bankAccounts` → `bankAccounts`. */
export function listName(list: string): string {
  return list.substring(list.lastIndexOf(".") + 1);
}

/** The keys of a new entry of [list]: the next free index with every field its entries have. */
export function nextListEntryKeys(
  params: PropertyParam[],
  list: string
): string[] {
  const entries = params
    .map((param) => parseListEntryKey(param.key))
    .filter((entry): entry is ListEntryKey => entry?.list === list);
  if (entries.length === 0) return [];
  const next = Math.max(...entries.map((entry) => entry.index)) + 1;
  const fields = [...new Set(entries.map((entry) => entry.field))];
  return fields.map((field) => `${list}[${next}]${field}`);
}

/**
 * The parameter of a new list entry, taken over from the entry of the same field in [params] (whether
 * it is live or a secret, its explanation); null if there is none.
 */
export function newListEntryParam(
  params: PropertyParam[],
  key: string
): PropertyParam | null {
  const entry = parseListEntryKey(key);
  if (!entry) return null;
  const sibling = params.findLast((param) => {
    const other = parseListEntryKey(param.key);
    return other?.list === entry.list && other.field === entry.field;
  });
  if (!sibling) return null;
  return {
    key,
    description: sibling.description,
    example: sibling.example,
    optional: true,
    known: false,
    status: "VARIANT",
    type: sibling.type,
    secret: sibling.secret,
    inFile: false,
    activeDiffers: false,
    live: sibling.live,
  };
}

/** The keys of the entry [index] of [list], one per field. */
export function listEntryKeys(
  params: PropertyParam[],
  list: string,
  index: number
): string[] {
  return params
    .map((param) => param.key)
    .filter((key) => {
      const entry = parseListEntryKey(key);
      return entry?.list === list && entry.index === index;
    });
}
