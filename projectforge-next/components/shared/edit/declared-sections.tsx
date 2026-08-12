"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { SectionDef } from "@/lib/page-def/types";
import { DeclaredFormField, fieldKey } from "./declared-form-field";

/**
 * One card of the edit page, rendered from its declaration: order, grouping, width and label.
 *
 * Which component each field gets is [DeclaredFormField]'s decision; this is only the card and the
 * three-column grid its fields sit in.
 */
export function DeclaredSection<M extends EntityMetadata>({
  section,
  metadata,
  id,
}: {
  section: SectionDef<M>;
  metadata: M;
  /** id of the entity being edited, null while adding — what a `render` body needs. */
  id: number | null;
}) {
  const t = useTranslations();
  return (
    <SectionCard>
      <SectionHeader title={t(section.titleKey)} />
      {section.render ? (
        section.render({ id })
      ) : (
        <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
          {section.fields?.map((field) => (
            <DeclaredFormField
              key={fieldKey(field)}
              field={field}
              metadata={metadata}
            />
          ))}
        </div>
      )}
    </SectionCard>
  );
}
