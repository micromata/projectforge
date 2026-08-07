import type { DataObject } from "@/lib/dynamic/path";
import { getByPath } from "@/lib/dynamic/path";

/**
 * `UIAgGrid.getRowClass` is a JavaScript source string that AG-Grid used to eval.
 * We don't execute server-supplied code, so the shapes the backend actually emits
 * are recognised by pattern and turned into a predicate over the row.
 *
 * All senders (`withGetRowClass`, eight call sites) write simple field
 * predicates: a truthy field, an equality or inequality against a string, a
 * membership test, or a numeric comparison. The right fix is server-side —
 * `getRowClass: String` should become a structured `rowHighlights` list in
 * UIAgGrid.kt — and this table is the bridge until then.
 */

/** The row highlight classes globals.css defines. */
const KNOWN_CLASSES = new Set([
  "row-deleted",
  "row-red",
  "row-green",
  "row-blue",
  "row-grey",
]);

/**
 * Two senders (banking, marketing) prefix the class with AG-Grid's own `ag-`
 * namespace, which the legacy stylesheet happened to define as well. Both spell
 * the same four colours, so the prefix is simply stripped.
 */
function normalizeClass(className: string): string | undefined {
  const name = className.startsWith("ag-row-")
    ? className.slice("ag-".length)
    : className;
  return KNOWN_CLASSES.has(name) ? name : undefined;
}

type RowPredicate = (row: DataObject) => boolean;

interface Rule {
  className: string;
  matches: RowPredicate;
}

/**
 * Builds the `rowClassName` callback for a grid. Returns undefined if the source
 * has no recognised rule at all, so DataTable can skip the whole mechanism.
 */
export function rowClassNameFor(
  source: string | undefined,
  gridId: string | undefined
): ((row: DataObject) => string | undefined) | undefined {
  const rules = parseRules(source, gridId);
  if (rules.length === 0) return undefined;
  // First match wins, mirroring the if/else-if chain the source is built from.
  return (row) => rules.find((rule) => rule.matches(row))?.className;
}

/**
 * Splits the source into `if (<condition>) { return '<class>'; }` branches and
 * translates each condition. `withGetRowClass` chains them with `else`, so the
 * order of appearance is the order of precedence.
 */
function parseRules(
  source: string | undefined,
  gridId: string | undefined
): Rule[] {
  if (!source) return [];
  const rules: Rule[] = [];
  const branch = /if\s*\(([^)]*)\)\s*\{\s*return\s*'([\w-]+)'/g;
  let unmatched = 0;
  for (const [, condition, rawClass] of source.matchAll(branch)) {
    const matches = parseCondition(condition);
    const className = normalizeClass(rawClass);
    if (!matches || !className) {
      unmatched++;
      continue;
    }
    rules.push({ className, matches });
  }
  if (unmatched > 0 && process.env.NODE_ENV !== "production") {
    console.warn(
      `[dynamic-grid] ${unmatched} row-class rule(s) of grid "${gridId}" not understood; those rows stay unhighlighted.`
    );
  }
  return rules;
}

/** `params.node.data?.deleted` / `params.node.data.address.isFavoriteCard` → path. */
const FIELD = /^params\.node\.data\??\.([\w.?]+)$/;

function fieldPath(expression: string): string | undefined {
  const match = FIELD.exec(expression.trim());
  return match?.[1].replace(/\?/g, "");
}

function parseCondition(condition: string): RowPredicate | undefined {
  const text = condition.trim();

  // ['A', 'B'].includes(params.node.data.status)
  const includes = /^\[([^\]]*)]\.includes\((.+)\)$/.exec(text);
  if (includes) {
    const path = fieldPath(includes[2]);
    if (!path) return undefined;
    const values = new Set(
      includes[1]
        .split(",")
        .map((value) => value.trim().replace(/^'|'$/g, ""))
        .filter(Boolean)
    );
    return (row) => values.has(String(getByPath(row, path) ?? ""));
  }

  // params.node.data.status === 'NEW' / !== 'BEZAHLT' / >= 0
  const comparison = /^(.+?)\s*(===|!==|==|!=|>=|<=|>|<)\s*(.+)$/.exec(text);
  if (comparison) {
    const path = fieldPath(comparison[1]);
    if (!path) return undefined;
    const operator = comparison[2];
    const literal = parseLiteral(comparison[3]);
    if (literal === undefined) return undefined;
    return (row) => compare(getByPath(row, path), operator, literal);
  }

  // !params.node.data.bezahlDatum
  if (text.startsWith("!")) {
    const path = fieldPath(text.slice(1));
    return path ? (row) => !getByPath(row, path) : undefined;
  }

  // params.node.data.conflict
  const path = fieldPath(text);
  return path ? (row) => Boolean(getByPath(row, path)) : undefined;
}

/** Only the literal kinds the senders use: quoted string, number, boolean, null. */
function parseLiteral(text: string): unknown {
  const value = text.trim();
  if (/^'.*'$/.test(value) || /^".*"$/.test(value)) return value.slice(1, -1);
  if (/^-?\d+(\.\d+)?$/.test(value)) return Number(value);
  if (value === "true") return true;
  if (value === "false") return false;
  if (value === "null") return null;
  return undefined;
}

function compare(value: unknown, operator: string, literal: unknown): boolean {
  switch (operator) {
    case "===":
    case "==":
      // A missing field must compare equal to null, as it would in JS.
      return literal === null
        ? value == null
        : normalize(value, literal) === literal;
    case "!==":
    case "!=":
      return literal === null
        ? value != null
        : normalize(value, literal) !== literal;
    case ">=":
      return Number(value) >= Number(literal);
    case "<=":
      return Number(value) <= Number(literal);
    case ">":
      return Number(value) > Number(literal);
    case "<":
      return Number(value) < Number(literal);
    default:
      return false;
  }
}

/**
 * Coerces the field to the literal's type, so an enum arriving as something other
 * than a string still matches `=== 'NEW'`. Booleans are compared strictly: the
 * senders test `isAddressValid === false`, and a field the server omitted
 * (JsonInclude.NON_NULL) must not count as false there — that is what the
 * original JavaScript did too.
 */
function normalize(value: unknown, literal: unknown): unknown {
  if (value == null) return value;
  if (typeof literal === "string") return String(value);
  if (typeof literal === "number") return Number(value);
  return value;
}
