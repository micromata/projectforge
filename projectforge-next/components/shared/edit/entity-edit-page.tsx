"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import {
  useCancelEntityEdit,
  useDeleteEntity,
  useEntityAction,
  useEntityDetail,
  useSaveEntity,
  type EntityWithId,
} from "@/hooks/use-entity-detail";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useFocusFirstField } from "@/hooks/use-focus-first-field";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import { useLegacyEditUrl } from "@/hooks/use-legacy-edit-url";
import { useInsertAccess } from "@/hooks/use-insert-access";
import {
  CLONE_PARAM,
  setPendingClone,
  usePendingClone,
} from "@/hooks/use-pending-clone";
import { useReadAccessGuard } from "@/hooks/use-read-access-guard";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EditablePageDef } from "@/lib/page-def/types";
import { entityAccess } from "@/lib/rs/entity-access";
import { cloneEntity } from "@/lib/rs/entity";
import { DeclaredSection } from "./declared-sections";
import { EntityCloneButton } from "./entity-clone-button";
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
  page: EditablePageDef<Row, Values, Data, M>;
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
  const {
    data: loaded,
    isLoading,
    isError,
    error,
  } = useEntityDetail<Data>(page.entity, id);
  // An add page opened by the clone button starts from the clone instead of from the backend's preset
  // (see runClone). Recognised by `?clone=1`, so a reload or a later add is a plain add again.
  const clone = usePendingClone<Data>(page.entity, id == null);
  const data = clone ?? loaded;
  // Whether this user may see this entity at all. Not the same question as the write access below: that
  // one hides the save and delete buttons of an entry the user may read, this one keeps the page from
  // existing. The list meta data it reads is the one `useLegacyEditUrl` already loads.
  const readAccess = useReadAccessGuard(page.entity, error);
  const saveMutation = useSaveEntity<Data>(page.entity, writeOptions);
  const deleteMutation = useDeleteEntity<Data>(page.entity, writeOptions);
  const actionMutation = useEntityAction<Data>(page.entity, writeOptions);
  const cancelMutation = useCancelEntityEdit<Data>(page.entity, writeOptions);
  const legacyUrl = useLegacyEditUrl(page.entity, id);
  // Cloning writes nothing, but it produces a *new* entry, so it needs the insert right and not the
  // write right of the entry on screen.
  const canInsert = useInsertAccess(page.entity);
  const [isCloning, setCloning] = useState(false);
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

  // What the backend says this user may do with this entry — the counterpart of Wicket's
  // `AbstractEditForm.updateButtonVisibility`, which hides the save button without update access and
  // the delete button without delete access.
  const access = entityAccess(data, id == null);

  // Return, and CTRL-Return inside a textarea, save — as the default button of a Wicket form does.
  // Under the same condition the save button carries, so the shortcut is never the looser way in —
  // including the write access, or it would submit a form that offers no save button (Wicket makes
  // cancel the default button in that case, see AbstractEditForm.updateButtonVisibility).
  const onKeyDown = useSubmitShortcut(
    () => void form.handleSubmit(),
    isDirty && !isSubmitting && access.write
  );

  /**
   * Leaves the page without saving — and tells the backend so, which is what makes the list mark the
   * entry the user was looking at (`onCancelEdit`, same as after a save).
   *
   * Awaited, so the list is refetched with the id already remembered; a cancel the server never
   * answers must still leave the page, hence the caught error. A new entry has no id to mark and
   * nothing to report, so it skips the call.
   */
  async function runCancel(): Promise<void> {
    if (id != null && data) {
      await cancelMutation.mutateAsync(data).catch(() => undefined);
    }
    router.push(page.route);
  }

  async function runDelete(): Promise<void> {
    if (!data) return;
    const result = await deleteMutation.mutateAsync(data);
    if (result.kind === "validationErrors") {
      // Nothing was deleted; the server explains why (e.g. the entry is still referenced).
      result.validationErrors.forEach((error) => toast.error(error.message));
      return;
    }
    if (result.kind === "rejected") {
      // The delete was refused, not merely invalid — an AccessException (see lib/rs/entity.ts).
      toast.error(result.message || t("validation.error.generic"));
      return;
    }
    toast.success(t("message.successfullChanged"));
    router.push(page.route);
  }

  /**
   * Opens a new entry built from this one — Wicket's `RechnungEditPage.cloneData`.
   *
   * Posted are the *form's current values*, not the loaded entity, so unsaved edits travel; neither
   * side validates them (Wicket's `ignoreErrorOnClone` says the same), because a clone is a starting
   * point and not a write.
   *
   * The prepared clone is handed to the add page outside React (see usePendingClone) — under
   * `output: "export"` no state rides along a navigation.
   */
  async function runClone(): Promise<void> {
    setCloning(true);
    try {
      const prepared = await cloneEntity<Data>(
        page.entity,
        form.state.values as unknown as Data
      );
      setPendingClone(page.entity, prepared);
      // The parameter is what tells the add page to take it (see usePendingClone) — and what keeps a
      // later plain `/new` a plain add, without the handover having to clear itself on the first read.
      router.push(`${page.route}/new?${CLONE_PARAM}=1`);
    } catch {
      // Nothing was written and the page stays, so the form is still the way forward.
      toast.error(t("validation.error.generic"));
    } finally {
      setCloning(false);
    }
  }

  // Before the two branches below: a refused read is an error here as well, and "not found" would be
  // the wrong thing to say about an entry the user merely may not see.
  if (readAccess.denied) {
    return null;
  }
  if (isLoading || readAccess.isPending) {
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
        onKeyDown={onKeyDown}
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
              onCancel={() => void runCancel()}
              saveOption={edit.saveOption && <edit.saveOption />}
              // Only for a stored entry: cloning an entry that isn't saved yet would copy a form the
              // user can simply keep filling in. `canInsert` is undefined until `listMeta` is there,
              // which keeps the button out of a page whose access is not known yet.
              cloneAction={
                edit.clone && id != null && canInsert ? (
                  <EntityCloneButton
                    onClone={runClone}
                    disabled={isSubmitting || isCloning}
                  />
                ) : undefined
              }
              // Nothing to delete before the first save. On `id` rather than on `data`: a new entry
              // has data too — the preset the backend answers `fetchNew` with (see useEntityDetail).
              deleteAction={
                id != null && data && access.delete ? (
                  <EntityDeleteButton
                    onDelete={runDelete}
                    disabled={isSubmitting || deleteMutation.isPending}
                  />
                ) : undefined
              }
              canSave={access.write}
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
