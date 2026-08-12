"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import {
  useDeleteEntity,
  useEntityAction,
  useEntityDetail,
  useSaveEntity,
  type EntityWithId,
} from "@/hooks/use-entity-detail";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useFocusFirstField } from "@/hooks/use-focus-first-field";
import { useLegacyEditUrl } from "@/hooks/use-legacy-edit-url";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { PageDef } from "@/lib/page-def/types";
import { DeclaredSection } from "./declared-sections";
import { EntityDeleteButton } from "./entity-delete-button";
import { EntityEditActions } from "./entity-edit-actions";
import { EntityEditHeader } from "./entity-edit-header";
import { entityTabs } from "./entity-tabs";

export interface EntityEditPageProps<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: PageDef<Row, Values, Data, M>;
  /** null adds a new entry: nothing is fetched and the form starts out blank. */
  id: number | null;
}

/**
 * The whole edit page of an entity, rendered from its declaration: load, form, sections, tabs,
 * save, delete.
 *
 * An entity with writes of its own (a book's lend-out) keeps composing `useEntityEditForm` itself —
 * the hooks stay public, and this component is nothing but their normal case.
 */
export function EntityEditPage<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({ page, id }: EntityEditPageProps<Row, Values, Data, M>) {
  const router = useRouter();
  const t = useTranslations();
  const { edit } = page;
  const writeOptions = { listQueryKey: page.queryKey };

  // A new entry has nothing to load — the hook stays disabled for id null.
  const { data, isLoading, isError } = useEntityDetail<Data>(page.entity, id);
  const saveMutation = useSaveEntity<Data>(page.entity, writeOptions);
  const deleteMutation = useDeleteEntity<Data>(page.entity, writeOptions);
  const actionMutation = useEntityAction<Data>(page.entity, writeOptions);
  const legacyUrl = useLegacyEditUrl(page.entity, id);
  // Adding an entry starts in the first field; editing one leaves the focus alone.
  const formRef = useFocusFirstField<HTMLFormElement>(id == null);

  const { form, isDirty, isSubmitting } = useEntityEditForm<Values, Data>({
    data,
    toFormValues: edit.toFormValues,
    defaultValues: edit.defaultValues(),
    schema: edit.schema,
    fieldNames: edit.fieldNames,
    arrayFieldNames: edit.arrayFieldNames,
    listRoute: page.route,
    savedMessage: t(edit.savedMessageKey),
    // The form's values are the DTO the backend expects — the type only differs in what it makes
    // optional (see the entity's schema file).
    save: (values, meta) => {
      const data = values as unknown as Data;
      // A declared action posts to `/rs/{entity}/{action}`; anything else is a save. An action name
      // the declaration doesn't list can only come from a typo in a button, and saving instead of
      // posting to a route that doesn't exist is the harmless of the two.
      return edit.actions?.includes(meta.action)
        ? actionMutation.mutateAsync({ action: meta.action, data })
        : saveMutation.mutateAsync(data);
    },
  });

  async function runDelete(): Promise<void> {
    if (!data) return;
    const result = await deleteMutation.mutateAsync(data);
    if (result.kind === "validationErrors") {
      // Nothing was deleted; the server explains why (e.g. the entry is still referenced).
      result.validationErrors.forEach((error) => toast.error(error.message));
      return;
    }
    toast.success(t("message.successfullChanged"));
    router.push(page.route);
  }

  if (isLoading) {
    return <Centered>{t("loading")}</Centered>;
  }
  if (id != null && (isError || !data)) {
    return <Centered>{t("entityEdit.notFound")}</Centered>;
  }

  const tabs = entityTabs({
    sections: edit.sections,
    t,
    id: data?.id ?? null,
    route: page.route,
    history: page.metadata.historizable,
    extraTabs: edit.extraTabs,
    onFormPage: true,
  });

  return (
    <EntityEditFormProvider value={{ form, metadata: page.metadata }}>
      <form
        ref={formRef}
        onSubmit={(e) => {
          e.preventDefault();
          void form.handleSubmit();
        }}
        className="flex min-w-0 flex-1 flex-col overflow-hidden"
      >
        <EditPageShell
          header={
            <EntityEditHeader
              listRoute={page.route}
              listLabel={t(page.titleKey)}
              title={data ? edit.title(data) : t(edit.newTitleKey)}
              trailing={edit.headerTrailing?.(data)}
              legacyUrl={legacyUrl}
            />
          }
          tabs={tabs}
          banner={edit.editBanner && <edit.editBanner />}
          sections={edit.sections.map((section) => (
            <DeclaredSection
              key={section.id}
              section={section}
              metadata={page.metadata}
              id={data?.id ?? null}
            />
          ))}
          actions={
            <EntityEditActions
              onCancel={() => router.push(page.route)}
              saveOption={edit.saveOption && <edit.saveOption />}
              // Nothing to delete before the first save. On `id` rather than on `data`: a new entry
              // has data too — the preset the backend answers `fetchNew` with (see useEntityDetail).
              deleteAction={
                id != null && data ? (
                  <EntityDeleteButton
                    onDelete={runDelete}
                    disabled={isSubmitting || deleteMutation.isPending}
                  />
                ) : undefined
              }
              isSaving={isSubmitting}
              isDirty={isDirty}
              // Saving leaves the page, so this is always the write before the one being made now:
              // `lastUpdate` from the backend, falling back to `created` for an entry never changed
              // since (both are on `BaseDTO`, so every entity carries them).
              lastSaved={
                (
                  data as
                    | { lastUpdate?: string | null; created?: string | null }
                    | undefined
                )?.lastUpdate ??
                (data as { created?: string | null } | undefined)?.created ??
                null
              }
            />
          }
        />
      </form>
    </EntityEditFormProvider>
  );
}

function Centered({ children }: { children: string }) {
  return (
    <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
      {children}
    </div>
  );
}
