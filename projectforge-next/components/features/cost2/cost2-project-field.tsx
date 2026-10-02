"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchOne } from "@/lib/rs/client";
import { leafKeyOf } from "@/lib/leaf-key";

/** The parts of a project's own cost number, as the Project DTO carries them (org...dto.Project). */
interface ProjectNumberParts {
  nummernkreis?: number | null;
  bereich?: number | null;
  nummer?: number | null;
}

/**
 * Picks the project a cost unit belongs to and fills the first three number parts from it, mirroring
 * Wicket's `Kost2EditForm.setProjekt`: `nummernkreis`/`bereich` are the project's own, `teilbereich` is
 * its number. `ProjektDO` has no `UIDataType`, so it carries no metadata (hence `metadataLess`) and the
 * value is bound as a bare reference (see cost2-schema.ts). The number is required for number ranges 4
 * and 5 (`fibu.kost2.error.projektNeededForNummernkreis`), which is what this spares the user typing.
 */
export function Cost2ProjectField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();

  async function fillFromProject(project: EntityRef | null) {
    if (!project) return;
    // Read after the pick rather than from the autosearch result: `project/autosearch` answers
    // `DisplayObject`s (id and display name only), so the number parts have to be fetched.
    const detail = await fetchOne<ProjectNumberParts>("project", project.id);
    form.setFieldValue("nummernkreis", detail.nummernkreis ?? null);
    form.setFieldValue("bereich", detail.bereich ?? null);
    form.setFieldValue("teilbereich", detail.nummer ?? null);
  }

  return (
    <EntityAutocompleteField
      name="project"
      label={t(leafKeyOf("fibu.projekt", t.has))}
      entity="project"
      metadataLess
      className={className}
      onPicked={(project) => void fillFromProject(project)}
    />
  );
}
