"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { InputField } from "@/components/shared/form/input-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchOne } from "@/lib/rs/client";
import { cn } from "@/lib/utils";

/** The fields of the project this fills in — a `Project` DTO as `/rs/project/{id}` answers it. */
interface ProjectDetail {
  customer?: EntityRef | null;
  projectManager?: EntityRef | null;
  headOfBusinessManager?: EntityRef | null;
  salesManager?: EntityRef | null;
}

/**
 * The project, the customer and the free-text customer of an order — three fields that only make sense
 * together.
 *
 * Custom rather than declared, twice over: `customer` and `project` reference `KundeDO`/`ProjektDO`, for
 * which there is no `UIDataType`, so the generated metadata cannot carry them however the entity is
 * annotated (hence `metadataLess`); and picking a project fills in what the project knows — its
 * customer and its three managers — which is a rule between fields, not a property of one.
 *
 * The autofill only ever fills what is **empty**: an order may deliberately name a different customer
 * than its project does (`fibu.auftrag.hint.kannVonProjektKundenAbweichen`) or a stand-in manager, and
 * overwriting that would quietly undo the user's choice. The free-text customer blocks the customer
 * being filled in for the same reason — it is what someone typed because no customer record fits.
 */
export function CustomerProjectFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();

  async function fillFromProject(project: EntityRef | null) {
    if (!project) return;
    // Read after the pick rather than from the autosearch result: `{entity}/autosearch` answers
    // `DisplayObject`s (id and display name only), so the managers have to be fetched.
    const detail = await fetchOne<ProjectDetail>("project", project.id);
    fillIfEmpty("projectManager", detail.projectManager);
    fillIfEmpty("headOfBusinessManager", detail.headOfBusinessManager);
    fillIfEmpty("salesManager", detail.salesManager);
    if (!form.getFieldValue("kundeText")) {
      fillIfEmpty("customer", detail.customer);
    }
  }

  function fillIfEmpty(name: string, value: EntityRef | null | undefined) {
    if (!value || form.getFieldValue(name)) return;
    form.setFieldValue(name, value);
  }

  return (
    // A grid of its own, with the columns and gaps of the section's: the three fields read as one row
    // beside each other, aligned with the rows above and below, while the block itself takes the width
    // its declaration gives it (`span: 3`, hence the className).
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      <EntityAutocompleteField
        name="project"
        label={t("fibu.projekt._")}
        entity="project"
        metadataLess
        onPicked={(project) => void fillFromProject(project)}
      />
      <EntityAutocompleteField
        name="customer"
        label={t("fibu.kunde._")}
        entity="customer"
        metadataLess
      />
      <InputField
        name="kundeText"
        label={t("fibu.kunde.text")}
        // Says what the field is for: a customer that has no record of its own. The backend drops it
        // when a customer *is* chosen (`OrderEntityRest.transformForDB`), so the two cannot disagree.
        hint={t("fibu.auftrag.hint.kannVonProjektKundenAbweichen")}
      />
    </div>
  );
}
