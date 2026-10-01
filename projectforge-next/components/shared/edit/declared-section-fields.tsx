"use client";

import type { EntityMetadata } from "@/lib/metadata/types";
import type { FieldDeclaration, SectionDef } from "@/lib/page-def/types";
import { cn } from "@/lib/utils";
import { DeclaredFormField, fieldKey } from "./declared-form-field";

const GRID = "grid grid-cols-1 gap-x-6 gap-y-4";

/**
 * The fields of a declared section: one three-column grid, or — with an [SectionDef.aside] — the main
 * fields in the left two columns and the aside stacked in the third, as two grids of their own so their
 * rows never interleave.
 */
export function DeclaredSectionFields<M extends EntityMetadata>({
  section,
  metadata,
}: {
  section: SectionDef<M>;
  metadata: M;
}) {
  if (!section.aside) {
    return (
      <FieldGrid
        fields={section.fields}
        metadata={metadata}
        className="md:grid-cols-3"
      />
    );
  }
  return (
    <div className={cn(GRID, "md:grid-cols-3")}>
      <FieldGrid
        fields={section.fields}
        metadata={metadata}
        className={cn(
          "md:col-span-2 md:grid-cols-2",
          section.mainColumns === 4 && "lg:grid-cols-4"
        )}
      />
      {/* `content-start`: a shorter column keeps its rows at the top instead of spreading them out. */}
      <FieldGrid
        fields={section.aside}
        metadata={metadata}
        className="content-start"
      />
    </div>
  );
}

function FieldGrid<M extends EntityMetadata>({
  fields,
  metadata,
  className,
}: {
  fields: FieldDeclaration<M>[] | undefined;
  metadata: M;
  className?: string;
}) {
  return (
    <div className={cn(GRID, className)}>
      {fields?.map((field) => (
        <DeclaredFormField
          key={fieldKey(field)}
          field={field}
          metadata={metadata}
        />
      ))}
    </div>
  );
}
