"use client";

import { useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { Spinner } from "@/components/shared/spinner";
import {
  useEntityDetail,
  useSaveEntity,
  type EntityWithId,
} from "@/hooks/use-entity-detail";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import { DeclaredSection } from "./declared-sections";
import { EntityEditActions } from "./entity-edit-actions";
import type { EntityEditDialogProps } from "./entity-edit-dialog";

/**
 * The form inside [EntityEditDialog] — a component of its own so that closing the dialog unmounts
 * every bit of it, form state and loaded preset alike.
 *
 * The same three pieces the edit page is made of: the preset an "add" starts from
 * (`useEntityDetail` for id null), the form around it, and the entity's declared sections.
 */
export function EntityEditDialogForm<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({
  page,
  onOpenChange,
  prefill,
  onSaved,
}: Omit<EntityEditDialogProps<Row, Values, Data, M>, "open">) {
  const t = useTranslations();
  const { edit } = page;
  const { data: preset, isLoading } = useEntityDetail<Data>(page.entity, null);
  const saveMutation = useSaveEntity<Data>(page.entity, {
    listQueryKey: page.queryKey,
  });

  // The values rather than their object identity decide what is written over the preset: a caller may
  // build the prefill inline, and a new object of the same values must not re-seed the form (which
  // would throw away what the user has typed). Serialized once, so the string is both the comparison
  // and the value.
  const prefillKey = JSON.stringify(prefill ?? null);
  const presetValues = useCallback(
    () => (JSON.parse(prefillKey) as Partial<Values> | null) ?? {},
    [prefillKey]
  );
  const toFormValues = useCallback(
    (data: Data) =>
      ({ ...edit.toFormValues(data), ...presetValues() }) as Values,
    [edit, presetValues]
  );
  const defaultValues = useMemo(
    () => ({ ...edit.defaultValues(), ...presetValues() }) as Values,
    [edit, presetValues]
  );

  const { form, isDirty, isSubmitting } = useEntityEditForm<Values, Data>({
    data: preset,
    toFormValues,
    defaultValues,
    schema: edit.schema,
    fieldNames: edit.fieldNames,
    arrayFieldNames: edit.arrayFieldNames,
    // Never navigated to: `onSaved` takes the place of leaving, and cancel closes the dialog.
    listRoute: page.route,
    onSaved: (id, values) => {
      onOpenChange(false);
      onSaved(id, values);
    },
    savedMessage: t(edit.savedMessageKey),
    save: (values) => saveMutation.mutateAsync(values as unknown as Data),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-10">
        <Spinner />
      </div>
    );
  }

  return (
    <EntityEditFormProvider value={{ form, metadata: page.metadata }}>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void form.handleSubmit();
        }}
      >
        <div className="flex max-h-[60vh] flex-col gap-4 overflow-auto px-6 py-4">
          {edit.sections.map((section) => (
            <DeclaredSection
              key={section.id}
              section={section}
              metadata={page.metadata}
              // Nothing exists yet, and a section's own body reads this as "adding".
              id={null}
            />
          ))}
        </div>
        {/* The bar of the edit page, minus what a dialog has no use for: no delete, no clone and no
            "last saved" — the entry is new. */}
        <EntityEditActions
          onCancel={() => onOpenChange(false)}
          canSave
          isSaving={isSubmitting}
          isDirty={isDirty}
          lastSaved={null}
        />
      </form>
    </EntityEditFormProvider>
  );
}
