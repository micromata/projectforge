"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { AccessValues } from "../schema";
import { ACCESS_TYPES, ACCESS_TYPE_LABEL_KEY } from "../types";

/** The four operation flags of a row, in the order the Wicket table shows the columns. */
const OPERATIONS = [
  { key: "accessSelect", labelKey: "access.type.select" },
  { key: "accessInsert", labelKey: "access.type.insert" },
  { key: "accessUpdate", labelKey: "access.type.update" },
  { key: "accessDelete", labelKey: "access.type.delete" },
] as const;

/**
 * The permission matrix — four access types (rows) by four SQL operations (columns) of boolean
 * checkboxes, the heart of the access-rights form. The Wicket `AccessEditTablePanel` counterpart, and
 * the first correct rendering of it: the old React `AccessTableComponent` was a dead stub of unbound
 * checkboxes (see the migration plan).
 *
 * A custom field because the matrix is a fixed-shape collection (`accessEntries`, normalized to four
 * rows in [toFormValues]) that no single metadata-driven field can describe; each cell binds to
 * `accessEntries[i].access*` in form state.
 */
export function AccessMatrix({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  // Read the whole matrix so a template button (which replaces the array) re-renders every cell.
  const entries = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.accessEntries
  );
  return (
    <div className={cn("overflow-x-auto", className)}>
      <table className="border-separate border-spacing-0 text-sm">
        <thead>
          <tr>
            <th className="px-3 py-2 text-left font-medium text-muted-foreground">
              {t(leafKeyOf("access.type", t.has))}
            </th>
            {OPERATIONS.map((op) => (
              <th
                key={op.key}
                className="px-3 py-2 text-center font-medium text-muted-foreground"
              >
                {t(op.labelKey)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {ACCESS_TYPES.map((type, rowIndex) => {
            const rowLabel = t(ACCESS_TYPE_LABEL_KEY[type]);
            return (
              <tr key={type}>
                <th
                  scope="row"
                  className="px-3 py-2 text-left font-normal text-foreground"
                >
                  {rowLabel}
                </th>
                {OPERATIONS.map((op) => {
                  const opLabel = t(op.labelKey);
                  const checked = entries[rowIndex]?.[op.key] === true;
                  return (
                    <td key={op.key} className="px-3 py-2 text-center">
                      <Checkbox
                        checked={checked}
                        disabled={readOnly}
                        aria-label={`${rowLabel} – ${opLabel}`}
                        onCheckedChange={(value) =>
                          form.setFieldValue(
                            `accessEntries[${rowIndex}].${op.key}`,
                            value === true
                          )
                        }
                      />
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<AccessValues, "accessEntries">;
}
