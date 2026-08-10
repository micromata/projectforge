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
      const isField = "name" in declaration;
      const name = isField ? declaration.name : declaration.id;
      const field =
        (isField ? metadata.fields[declaration.name] : undefined) ?? TEXT;

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
        meta: { label, align, filterKind: filterKind ?? undefined },
        size: declaration.size,
        minSize: declaration.minSize,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind={filterKind ?? undefined}
          >
            {headerLabel}
          </DataTableColumnHeader>
        ),
        cell:
          declaration.cell ??
          ((ctx) =>
            declaredCell(ctx.getValue(), field, {
              format,
              t: translate,
              className: declaration.className,
            })),
      } as ColumnDef<Row>;
    });
  }, [columns, metadata, t, format]);
}
