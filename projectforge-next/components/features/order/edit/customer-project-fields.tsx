"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { EntityOrTextField } from "@/components/shared/form/entity-or-text-field";
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
 * The project and the customer of an order — the customer either picked from the list or typed as free text
 * (see EntityOrTextField) — fields that only make sense together.
 *
 * Custom rather than declared, twice over: `customer` and `project` reference `KundeDO`/`ProjektDO`, for
 * which there is no `UIDataType`, so the generated metadata cannot carry them however the entity is
 * annotated (hence `metadataLess`); and picking a project fills in what the project knows — its
 * customer, and its three managers as the order's further contacts — which is a rule between fields, not a property of one.
 *
 * The autofill only ever fills what is **empty**: an order may deliberately name a different customer
 * than its project does (`fibu.auftrag.hint.kannVonProjektKundenAbweichen`) or other contacts, and
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
    fillAdditionalContactsIfEmpty([
      detail.projectManager,
      detail.headOfBusinessManager,
      detail.salesManager,
    ]);
    if (!form.getFieldValue("kundeText")) {
      fillIfEmpty("customer", detail.customer);
    }
  }

  /**
   * The project's managers as further contacts, if the order has none yet: distinct, and without the
   * main contact, who is one already.
   */
  function fillAdditionalContactsIfEmpty(managers: (EntityRef | null | undefined)[]) {
    const current = form.getFieldValue("additionalContacts") as EntityRef[] | null | undefined;
    if (current && current.length > 0) return;
    const contactPersonId = (form.getFieldValue("contactPerson") as EntityRef | null | undefined)?.id;
    const contacts: EntityRef[] = [];
    for (const manager of managers) {
      if (!manager || manager.id === contactPersonId) continue;
      if (contacts.some((contact) => contact.id === manager.id)) continue;
      contacts.push(manager);
    }
    if (contacts.length > 0) form.setFieldValue("additionalContacts", contacts);
  }

  function fillIfEmpty(name: string, value: EntityRef | null | undefined) {
    if (!value || form.getFieldValue(name)) return;
    form.setFieldValue(name, value);
  }

  return (
    // Stacked, with the row gap of the section's grid: the two fields are the top two rows of the
    // order's customer column (the head section's `aside`), one above the other like the rows below.
    <div className={cn("grid grid-cols-1 gap-y-4", className)}>
      <EntityAutocompleteField
        name="project"
        label={t("fibu.projekt._")}
        entity="project"
        metadataLess
        onPicked={(project) => void fillFromProject(project)}
      />
      <EntityOrTextField
        entityName="customer"
        textName="kundeText"
        label={t("fibu.kunde._")}
        entity="customer"
        metadataLess
        // Says what the free text is for: a customer that has no record of its own, which may differ
        // from the project's. Picking one clears the other, and the backend drops the free text beside a
        // customer as well (`OrderEntityRest.transformForDB`), so the two cannot disagree.
        hint={t("fibu.auftrag.hint.kannVonProjektKundenAbweichen")}
      />
    </div>
  );
}
