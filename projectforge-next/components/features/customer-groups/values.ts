import { compareText, type FormatContext } from "@/lib/format";
import type {
  BusinessUnitValues,
  CustomerGroupsData,
  CustomerGroupsValues,
  CustomerSetData,
  CustomerSetValues,
} from "./types";

const KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
const KEY_LENGTH = 6;

/**
 * A key for a new group or business unit, in the server's format (`CustomerGroupValidator.KEY_REGEX`).
 * Assigned here and not on save, so a business unit can refer to a group added in the same edit.
 */
export function newKey(used: ReadonlySet<string>): string {
  for (;;) {
    const bytes = crypto.getRandomValues(new Uint8Array(KEY_LENGTH));
    const key = Array.from(bytes, (b) => KEY_CHARS[b % KEY_CHARS.length]).join(
      ""
    );
    if (!used.has(key)) return key;
  }
}

export function usedKeys(values: CustomerGroupsValues): Set<string> {
  return new Set(
    [...values.groups, ...values.businessUnits].map((set) => set.key)
  );
}

function toSetValues(
  data: CustomerSetData,
  used: Set<string>
): CustomerSetValues {
  const key = data.key || newKey(used);
  used.add(key);
  return {
    key,
    name: data.name ?? "",
    customers: data.customers ?? [],
    texts: data.texts ?? [],
  };
}

/**
 * With [format], the groups and business units are sorted by name in the user's language — on load and
 * after a save, not while typing, so a row being renamed stays where it is.
 */
export function toFormValues(
  data: CustomerGroupsData,
  format?: FormatContext
): CustomerGroupsValues {
  const used = new Set<string>();
  const byName = <T extends CustomerSetValues>(sets: T[]) =>
    format ? sets.sort((a, b) => compareText(a.name, b.name, format)) : sets;
  return {
    groups: byName(
      (data.groups ?? []).map((group) => toSetValues(group, used))
    ),
    businessUnits: byName(
      (data.businessUnits ?? []).map(
        (bu): BusinessUnitValues => ({
          ...toSetValues(bu, used),
          groups: bu.groups ?? [],
          tasks: bu.tasks ?? [],
        })
      )
    ),
    remark: data.remark ?? "",
    lastUpdate: data.lastUpdate ?? null,
  };
}

export const EMPTY_VALUES: CustomerGroupsValues = {
  groups: [],
  businessUnits: [],
  remark: "",
  lastUpdate: null,
};

/**
 * What is posted: a business unit's reference to a group removed in this edit is dropped — the user
 * removed the group, and the server would refuse the dangling key.
 */
export function toPayload(values: CustomerGroupsValues): CustomerGroupsValues {
  const groupKeys = new Set(values.groups.map((group) => group.key));
  return {
    ...values,
    businessUnits: values.businessUnits.map((bu) => ({
      ...bu,
      groups: bu.groups.filter((key) => groupKeys.has(key)),
    })),
  };
}

/**
 * Whether a free-text member is acceptable, as `TextPattern.of` on the server: an exact name, or one
 * with a leading and/or trailing `*`; at least 2 further characters, no `*` inside, and no SQL wildcard
 * (`%`, `_`) in a pattern.
 */
export function isValidTextMember(raw: string): boolean {
  const text = raw.trim();
  const leading = text.startsWith("*");
  const trailing = text.length > 1 && text.endsWith("*");
  let plain = leading ? text.slice(1) : text;
  if (trailing) plain = plain.slice(0, -1);
  if (plain.trim().length < 2 || plain.trim() !== plain) return false;
  if (plain.includes("*")) return false;
  return !((leading || trailing) && /[%_]/.test(plain));
}
