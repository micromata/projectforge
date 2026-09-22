"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTableColumnHeader } from "@/components/data-table";
import { useFormatContext } from "@/hooks/use-format";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata, FieldMetadata } from "@/lib/metadata/types";
import {
  alignFor,
  filterKindFor,
  labelKeyFor,
} from "@/lib/page-def/define-page";
import type { ColumnDeclaration } from "@/lib/page-def/types";
import { declaredCell } from "./declared-cell";
import { periodColumnDef } from "./declared-period-column";

/** A field the entity has no metadata for (a computed column) is plain text. */
const TEXT: FieldMetadata = { dataType: "STRING", required: false };

/**
 * Turns a page's column declarations into TanStack column defs: the label comes from the field's
 * `i18nKey`, the filter and the alignment from its data type, and the cell from the default for that
 * type — unless the declaration renders its own.
 *
 * Memoised because the identity matters: a new array makes TanStack rebuild every column instance,
 * and the new `header` function identity remounts the header — which closes an open filter popover
 * on the very click that opened it. So `columns` must be a stable array too, which it is: a page
 * declaration is a module-level constant (see definePage).
 */
export function useDeclaredColumns<
  Row extends ListRow,
  M extends EntityMetadata,
>(metadata: M, columns: ColumnDeclaration<Row, M>[]): ColumnDef<Row>[] {
  const t = useTranslations();
  const format = useFormatContext();

  return useMemo<ColumnDef<Row>[]>(() => {
    const translate = t as unknown as ((key: string) => string) & {
      has: (key: string) => boolean;
    };
    return columns.map((declaration) => {
      // Two fields shown as one value, hence neither a field nor a computed column — see PeriodColumn.
      if ("periodLabelKey" in declaration)
        return periodColumnDef<Row, M>(
          declaration,
          metadata,
          translate,
          format
        );
      const isField = "name" in declaration;
      const name = isField ? declaration.name : declaration.id;
      // A computed column may name its data type, since it has no field to derive one from — that is
      // what makes an order's transient net sum read as money (see ComputedColumn.dataType).
      const field = isField
        ? (metadata.fields[declaration.name] ?? TEXT)
        : declaration.dataType
          ? { dataType: declaration.dataType, required: false }
          : TEXT;

      const label = translate(
        labelKeyFor(metadata, name, translate.has, declaration.labelKey)
      );
      const headerLabel = declaration.headerLabelKey
        ? translate(declaration.headerLabelKey)
        : label;
      // `null` in the declaration means "offer no filter"; leaving it out means "derive one".
      const filterKind =
        declaration.filterKind === undefined
          ? filterKindFor(field.dataType)
          : declaration.filterKind;
      const align = declaration.align ?? alignFor(field.dataType);

      return {
        ...(isField
          ? { accessorKey: name }
          : { id: name, accessorFn: declaration.accessor }),
        meta: {
          label,
          align,
          filterKind: filterKind ?? undefined,
          wrap: declaration.wrap,
        },
        // Left to the table's default (sortable) unless the declaration opts out — see
        // ColumnBase.sortable.
        ...(declaration.sortable === false ? { enableSorting: false } : {}),
        size: declaration.size,
        minSize: declaration.minSize,
        // Only where the declaration says so: the default is sortable, and the header renders no sort
        // control for a column that isn't (see ColumnBase.sortable).
        ...(declaration.sortable === false ? { enableSorting: false } : {}),
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind={filterKind ?? undefined}
          >
            {headerLabel}
          </DataTableColumnHeader>
        ),
        cell: (ctx) => {
          const rendered = declaration.cell
            ? declaration.cell(ctx)
            : declaredCell(ctx.getValue(), field, {
                format,
                t: translate,
                className: declaration.className,
                // The active search term, off the table meta (see useDataTable): every default text
                // cell highlights the match, without this builder knowing the term itself.
                highlight: ctx.table.options.meta?.highlight,
              });
          const tooltip = declaration.tooltip?.(ctx.row.original);
          if (!tooltip) return rendered;
          // A wrapper rather than an attribute on the rendered element: the cell may be the
          // declaration's own JSX, which this must not reach into. The table's one delegated tooltip
          // finds it by `closest` and prefers it over the clipped text (see useOverflowTooltip).
          return (
            <span className="block truncate" data-tooltip={tooltip}>
              {rendered}
            </span>
          );
        },
      } as ColumnDef<Row>;
    });
  }, [columns, metadata, t, format]);
}
