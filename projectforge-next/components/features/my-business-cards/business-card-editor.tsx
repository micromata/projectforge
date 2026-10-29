"use client";

import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { EntityEditActions } from "@/components/shared/edit/entity-edit-actions";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import { useUnsavedChangesWarning } from "@/hooks/use-unsaved-changes-warning";
import { saveBusinessCard } from "@/lib/rs/my-business-cards";
import { BusinessCardDeleteButton } from "./business-card-delete-button";
import { BusinessCardFields } from "./business-card-fields";
import { BusinessCardMainSection } from "./business-card-main-section";
import * as S from "./schema";
import type { BusinessCard } from "./types";
import { useAddressTakeover } from "./use-address-takeover";
import { useBusinessCardPreview } from "./use-business-card-preview";
import { useIncludedFields } from "./use-included-fields";

const EMPTY = S.toFormValues({});

/**
 * Edits one business card: the source address (the own one is suggested), per field whether it is on the card
 * and its text, the photo and the live QR code. The card keeps its own copy of the data.
 */
export function BusinessCardEditor({
  initial,
  suggestionAddressId,
  gatewayConfigured,
  onDone,
}: {
  initial: BusinessCard;
  suggestionAddressId: number | null;
  gatewayConfigured: boolean;
  onDone: () => void;
}) {
  const t = useTranslations();
  const isNew = !initial.id;
  const fields = useIncludedFields(() => S.initialIncluded(initial, isNew));
  const { form, isDirty, isSubmitting } = useEntityEditForm<
    S.BusinessCardValues,
    BusinessCard
  >({
    data: initial,
    toFormValues: S.toFormValues,
    defaultValues: EMPTY,
    schema: S.businessCardSchema,
    fieldNames: S.BUSINESS_CARD_FIELD_NAMES,
    listRoute: "/myBusinessCards",
    savedMessage: t("businessCard.saved"),
    onSaved: onDone,
    save: (values) => saveBusinessCard(S.postedCard(values, fields.current())),
  });
  // A changed tick is a change as well, though no value of the form.
  const changed = isDirty || fields.changed;
  useUnsavedChangesWarning(changed);
  const onKeyDown = useSubmitShortcut(
    () => void form.handleSubmit(),
    !isSubmitting
  );
  const values = useStore(
    form.store,
    (s: unknown) => (s as { values: S.BusinessCardValues }).values
  );
  const preview = useBusinessCardPreview(S.postedCard(values, fields.included));
  const takeover = useAddressTakeover(form, initial, values.addressId, (card) =>
    fields.set(S.initialIncluded(card, true))
  );
  const groupFields = (privateFields: boolean) => (
    <BusinessCardFields
      privateFields={privateFields}
      included={fields.included}
      onIncluded={fields.toggle}
      source={takeover.source}
    />
  );
  const cards = [
    {
      id: "card",
      label: t("businessCard.section.card"),
      content: (
        <BusinessCardMainSection
          address={takeover.address}
          suggestionAddressId={suggestionAddressId}
          onPickAddress={(ref) => void takeover.pick(ref)}
          includePhoto={values.includePhoto}
          gatewayConfigured={gatewayConfigured}
          preview={preview.data}
        />
      ),
    },
    {
      id: "business",
      label: t("businessCard.section.business"),
      content: groupFields(false),
    },
    {
      id: "private",
      label: t("businessCard.section.private"),
      content: groupFields(true),
    },
  ];
  return (
    <EntityEditFormProvider
      value={{
        form,
        metadata: S.BUSINESS_CARD_METADATA,
        readOnly: false,
        data: initial,
      }}
    >
      <form
        className="flex min-h-0 flex-1 flex-col overflow-hidden"
        onKeyDown={onKeyDown}
        onSubmit={(event) => {
          event.preventDefault();
          void form.handleSubmit();
        }}
      >
        <EditPageShell
          header={null}
          tabs={cards.map(({ id, label }) => ({ id, label }))}
          sections={cards.map(({ id, label, content }) => (
            <SectionCard key={id}>
              <SectionHeader title={label} />
              {content}
            </SectionCard>
          ))}
          actions={
            <EntityEditActions
              onCancel={onDone}
              canSave
              isSaving={isSubmitting}
              isDirty={changed}
              allowSaveUnchanged={isNew || fields.changed}
              isNew={isNew}
              lastSaved={null}
              deleteAction={
                initial.id ? (
                  <BusinessCardDeleteButton card={initial} onDeleted={onDone} />
                ) : undefined
              }
            />
          }
        />
      </form>
    </EntityEditFormProvider>
  );
}
