"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import {
  useCancelEntityEdit,
  useDeleteEntity,
  useEntityAction,
  useEntityDetail,
  useForceDeleteEntity,
  useSaveEntity,
  useUndeleteEntity,
  type EntityWithId,
  type NewEntryParams,
} from "@/hooks/use-entity-detail";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useFocusFirstField } from "@/hooks/use-focus-first-field";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import { useLegacyEditUrl } from "@/hooks/use-legacy-edit-url";
import { useInsertAccess } from "@/hooks/use-insert-access";
import { useHistoryCommentSupport } from "@/hooks/use-history-comment-support";
import { usePendingClone } from "@/hooks/use-pending-clone";
import { useReadAccessGuard } from "@/hooks/use-read-access-guard";
import { useUnsavedChangesWarning } from "@/hooks/use-unsaved-changes-warning";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EditablePageDef } from "@/lib/page-def/types";
import { entityAccess, type EntityAccessFlags } from "@/lib/rs/entity-access";
import { DeclaredSection } from "./declared-sections";
import { EntityCrossLinks } from "./entity-cross-links";
import { EntityDeletedBanner } from "./entity-deleted-banner";
import { EntityEditActionBar } from "./entity-edit-action-bar";
import { HistoryUserCommentField } from "./history-user-comment-field";
import { entityTabPanels } from "./entity-tab-panels";
import { entityTabs } from "./entity-tabs";
import type { EditPageTab } from "../edit-page-tabs";
import type { EditOutcome } from "./edit-outcome";
import { useEditRunActions } from "./use-edit-run-actions";

/**
 * The regions the host chrome lays out — everything the form is made of, computed once here so a page
 * and a modal differ only in how they arrange them (see [EntityEditBody]'s `renderShell`). Where the
 * user came from (the breadcrumb's list route) is the host's own concern and is not among them.
 */
export interface EditRegions {
  title: string;
  category: string;
  crossLinks?: ReactNode;
  legacyUrl?: string;
  trailing?: ReactNode;
  deleted: boolean;
  tabs: EditPageTab[];
  tabPanels: Record<string, ReactNode>;
  sections: (ReactNode | ((active: boolean) => ReactNode))[];
  belowSections?: ReactNode;
  banner?: ReactNode;
  actions: ReactNode;
}

export interface EntityEditBodyProps<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: EditablePageDef<Row, Values, Data, M>;
  /** null adds a new entry: nothing is fetched and the form starts out blank. */
  id: number | null;
  /** What an "add" starts from, resolved by the caller (the URL for a page, a descriptor for a modal). */
  newParams?: NewEntryParams;
  /** Values written over the preset — the wizard's "create group with this name" (see EntityEditModal). */
  prefill?: Partial<Values>;
  /**
   * Values applied over the loaded entry as a real, dirtying change — a calendar event opened on its
   * dragged/resized position, which is a move to persist and not a preset (see EntityEditModal).
   */
  dirtyPrefill?: Partial<Values>;
  outcome: EditOutcome;
  renderShell: (regions: EditRegions) => ReactNode;
  /** The `<form>`'s classes — a page fills its column, a modal fits its content box. */
  formClassName?: string;
  /** Shown while the entry loads; a page centers a word, a modal spins. */
  renderLoading?: () => ReactNode;
  /** The entry can't be shown (no read access, or it's gone) — a modal closes rather than sit empty. */
  onUnavailable?: () => void;
}

/**
 * The whole edit form of an entity — load, form, sections, tabs, save, delete — with the two things a
 * host varies injected: what happens after each way it ends ([EditOutcome]) and how its regions are
 * laid out (`renderShell`). [EntityEditPage] hosts it on a page, [EntityEditModal] in a dialog.
 */
export function EntityEditBody<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({
  page,
  id,
  newParams,
  prefill,
  dirtyPrefill,
  outcome,
  renderShell,
  formClassName = "flex min-w-0 flex-1 flex-col overflow-hidden",
  renderLoading,
  onUnavailable,
}: EntityEditBodyProps<Row, Values, Data, M>) {
  const t = useTranslations();
  const { edit } = page;
  const writeOptions = { listQueryKey: page.queryKey };

  // A new entry has nothing to load — the hook stays disabled for id null.
  const {
    data: loaded,
    isLoading,
    isError,
    error,
  } = useEntityDetail<Data>(page.entity, id, newParams);
  // An add opened by the clone button starts from the clone, recognised by `?clone=1` (see runClone).
  const clone = usePendingClone<Data>(page.entity, id == null);
  const data = clone ?? loaded;
  // Whether this user may see this entity at all — a different question than the write access below.
  const readAccess = useReadAccessGuard(page.entity, error);
  const saveMutation = useSaveEntity<Data>(page.entity, writeOptions);
  const deleteMutation = useDeleteEntity<Data>(page.entity, writeOptions);
  const forceDeleteMutation = useForceDeleteEntity<Data>(
    page.entity,
    writeOptions
  );
  const undeleteMutation = useUndeleteEntity<Data>(page.entity, writeOptions);
  const actionMutation = useEntityAction<Data>(page.entity, writeOptions);
  const cancelMutation = useCancelEntityEdit<Data>(page.entity, writeOptions);
  const legacyUrl = useLegacyEditUrl(page.entity, id);
  // Cloning produces a *new* entry, so it needs the insert right, not the write right of the entry.
  const canInsert = useInsertAccess(page.entity);
  const takesHistoryComment = useHistoryCommentSupport(page.entity);
  const [historyComment, setHistoryComment] = useState("");
  const formRef = useFocusFirstField<HTMLFormElement>(
    id == null,
    edit.autoFocus
  );

  // The prefill is written over the preset by value, so a caller may build it inline: a fresh object of
  // the same values must not re-seed the form and throw away what the user has typed (see EntityEditModal).
  const prefillKey = JSON.stringify(prefill ?? null);
  const presetValues = useCallback(
    () => (JSON.parse(prefillKey) as Partial<Values> | null) ?? {},
    [prefillKey]
  );
  const toFormValues = useCallback(
    (row: Data) => ({ ...edit.toFormValues(row), ...presetValues() }) as Values,
    [edit, presetValues]
  );
  const defaultValues = useMemo(
    () => ({ ...edit.defaultValues(), ...presetValues() }) as Values,
    [edit, presetValues]
  );

  const { form, isDirty, isSubmitting } = useEntityEditForm<Values, Data>({
    data,
    toFormValues,
    defaultValues,
    schema: edit.schema,
    fieldNames: edit.fieldNames,
    arrayFieldNames: edit.arrayFieldNames,
    // Never navigated to: `onSaved` takes the place of leaving (see EditOutcome).
    listRoute: page.route,
    onSaved: (savedId, values) => outcome.afterSave(savedId, values),
    savedMessage: t(edit.savedMessageKey),
    save: (values, meta) => {
      const posted = {
        ...values,
        // The change comment rides along as `BaseDTO.historyUserComment`; only when there is one.
        ...(historyComment.trim()
          ? { historyUserComment: historyComment }
          : {}),
        // And so does "deleted" where the entry is gone: a hand-built form posts its values *as* the
        // DTO, and a write that dropped `deleted` would silently bring the entry back to life. Only
        // ever added as `true`, so an ordinary entry's payload is unchanged.
        ...((data as EntityAccessFlags | undefined)?.deleted === true
          ? { deleted: true }
          : {}),
      } as unknown as Data;
      // A declared action posts to `/rs/{entity}/{action}`; anything else is a save.
      return edit.actions?.includes(meta.action)
        ? actionMutation.mutateAsync({ action: meta.action, data: posted })
        : saveMutation.mutateAsync(posted);
    },
  });

  // A moved calendar event opens already at its dragged/resized position, applied over the loaded
  // event as a real change — so Save is enabled and the move persists. Unlike `prefill`, which seeds
  // the clean baseline the form resets onto (a viewed occurrence, no change intended), this writes the
  // values *after* that reset has settled onto the backend's, marking the form dirty. The ref keeps a
  // later re-render — a side tab hidden and shown again, a refetch after an action — from re-applying
  // it and clobbering what the user then typed (see the reset effect in useEntityEditForm).
  const dirtyPrefillKey = JSON.stringify(dirtyPrefill ?? null);
  const dirtyPrefillDone = useRef(false);
  useEffect(() => {
    if (id == null || !data || dirtyPrefillDone.current) return;
    const values = JSON.parse(dirtyPrefillKey) as Partial<Values> | null;
    if (!values) return;
    dirtyPrefillDone.current = true;
    for (const [name, value] of Object.entries(values)) {
      form.setFieldValue(name, value);
    }
  }, [id, data, dirtyPrefillKey, form]);

  // The heading, following the live form as well as the loaded row: an entry named after a field it
  // holds (a time sheet after its task) re-titles the moment that field changes. On `id`, not on
  // `data`: a new entry has data too (the backend's `fetchNew` preset), and its `edit.title` is the
  // empty string. Selecting the title *string* means the header re-renders only when it changes, not
  // on every keystroke — the store would otherwise fire on all of them.
  const title = useStore(form.store, (state) =>
    id == null || !data
      ? t(edit.newTitleKey)
      : // eslint-disable-next-line @typescript-eslint/no-explicit-any
        edit.title(data, (state as any).values as Values)
  );

  // What the backend says this user may do with this entry — the counterpart of Wicket's
  // `AbstractEditForm.updateButtonVisibility`.
  const access = entityAccess(data, id == null);

  const {
    runCancel,
    runDelete,
    runForceDelete,
    isForceDeleting,
    runUndelete,
    runClone,
    isCloning,
    runConvert,
    isConverting,
  } = useEditRunActions({
    page,
    id,
    data,
    form,
    cancelMutation,
    deleteMutation,
    forceDeleteMutation,
    undeleteMutation,
    outcome,
  });

  // Return, and CTRL-Return in a textarea, save — under the same condition the save button carries.
  const onKeyDown = useSubmitShortcut(
    () => void form.handleSubmit(),
    isDirty && !isSubmitting && access.write
  );

  // Asks before a link or a reload throws the entries away — a modal reads the same armed flag to
  // confirm its own close (see confirmLeaveUnsavedChanges). Not for the deliberate ways out.
  useUnsavedChangesWarning(isDirty && !isSubmitting);

  // A modal can't sit on an entry the user may not see or that isn't there — it closes instead. On a
  // page the guard already redirects; here the host decides. Deleted entries are shown, so not this.
  const notFound = id != null && !isLoading && (isError || !data);
  useEffect(() => {
    if (onUnavailable && (readAccess.denied || notFound)) onUnavailable();
  }, [onUnavailable, readAccess.denied, notFound]);

  if (readAccess.denied) return null;
  if (isLoading || readAccess.isPending)
    return renderLoading ? (
      renderLoading()
    ) : (
      <Centered>{t("loading")}</Centered>
    );
  if (notFound) return <Centered>{t("entityEdit.notFound")}</Centered>;

  // A section whose subject the installation doesn't know is dropped here — missing from the tab strip
  // and the cards alike (see SectionDef.visible).
  const sections = edit.sections.filter(
    (section) =>
      section.visible?.({
        data: data as unknown as Record<string, unknown> | undefined,
      }) ?? true
  );

  const regions: EditRegions = {
    title,
    category: t(page.categoryKey),
    // Only for a stored entry: every cross link names it in its url (see CrossLinkDef).
    crossLinks:
      edit.crossLinks && id != null ? (
        <EntityCrossLinks links={edit.crossLinks} data={data} />
      ) : undefined,
    legacyUrl,
    trailing: edit.headerTrailing?.(data),
    deleted: access.deleted,
    tabs: entityTabs({
      sections,
      t,
      id: data?.id ?? null,
      history: page.metadata.historizable,
      extraTabs: edit.extraTabs,
    }),
    tabPanels: entityTabPanels({
      entity: page.entity,
      id: data?.id ?? null,
      history: page.metadata.historizable,
      extraTabs: edit.extraTabs,
    }),
    // A function per section, so a folded one learns its tab was clicked (see EditPageShell,
    // DeclaredSection). A deleted entry is shown, not edited: a disabled fieldset blocks every control
    // inside without a field having to know, since there is no save button to discard what was typed.
    sections: sections.map(
      (section) =>
        function renderSection(active: boolean) {
          const rendered = (
            <DeclaredSection
              key={section.id}
              section={section}
              metadata={page.metadata}
              id={data?.id ?? null}
              active={active}
            />
          );
          return access.deleted ? (
            <fieldset
              key={section.id}
              disabled
              className="contents"
              aria-label={t("deleted")}
            >
              {rendered}
            </fieldset>
          ) : (
            rendered
          );
        }
    ),
    // The deleted notice comes first and does not replace the entity's own banner.
    banner:
      access.deleted || edit.editBanner ? (
        <>
          {access.deleted && <EntityDeletedBanner />}
          {edit.editBanner && <edit.editBanner />}
        </>
      ) : undefined,
    // Below the sections: the entity's own footer note (the legacy `layoutBelowActions`, e.g. a time
    // sheet's AI-savings hint) and, only where the history takes a comment and the user may write, the
    // change-comment field — a comment on a save that cannot happen is nothing to ask for (see
    // LayoutUtils.processEditPage).
    belowSections:
      edit.editFooter || (takesHistoryComment && access.write) ? (
        <>
          {edit.editFooter && <edit.editFooter />}
          {takesHistoryComment && access.write && (
            <HistoryUserCommentField
              value={historyComment}
              onChange={setHistoryComment}
            />
          )}
        </>
      ) : undefined,
    actions: (
      <EntityEditActionBar
        onCancel={() => void runCancel()}
        saveOption={edit.saveOption && <edit.saveOption />}
        // Clone needs a stored entry and the insert right; `canInsert` is undefined until `listMeta`
        // is there, which keeps the button out until the access is known.
        showClone={Boolean(edit.clone && id != null && canInsert)}
        // On `id` rather than `data`: a new entry has data too (the `fetchNew` preset).
        showDelete={Boolean(id != null && data && access.delete)}
        // The irrevocable delete, beside the ordinary one: only where the entity opts in
        // (EditDef.forceDelete → isForceDeletionSupport) and the same access the mark-as-deleted needs,
        // and never while an entry is already deleted (undelete takes that place).
        showForceDelete={Boolean(
          edit.forceDelete &&
          id != null &&
          data &&
          access.delete &&
          !access.deleted
        )}
        // In the delete button's place, under the right legacy restores with — insert, not write.
        showUndelete={Boolean(
          id != null && data && access.deleted && canInsert
        )}
        // Wherever the entity declares a conversion — the backend adds its switch button to the layout
        // unconditionally too (TimesheetPagesRest/TeamEventPagesRest.createEditLayout), and it acts on
        // the form's values, so it needs no stored entry.
        showConvert={Boolean(edit.convert)}
        convertLabel={edit.convert ? t(edit.convert.labelKey) : ""}
        onClone={runClone}
        onDelete={runDelete}
        onForceDelete={runForceDelete}
        onUndelete={runUndelete}
        onConvert={runConvert}
        cloneDisabled={isSubmitting || isCloning}
        deleteDisabled={isSubmitting || deleteMutation.isPending}
        forceDeleteDisabled={isSubmitting || isForceDeleting}
        undeleteDisabled={isSubmitting || undeleteMutation.isPending}
        convertDisabled={isSubmitting || isConverting}
        canSave={access.write}
        isSaving={isSubmitting}
        isDirty={isDirty}
        // Saving leaves, so this is always the write before this one: `lastUpdate`, or `created` for
        // an entry never changed since (both on `BaseDTO`).
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
    ),
  };

  return (
    <EntityEditFormProvider
      // `readOnly` for a deleted entry: the fieldset above blocks input, this is what the fields read
      // to look the part (a select keeps its clear button otherwise, see useFormReadOnly).
      value={{
        form,
        metadata: page.metadata,
        readOnly: access.deleted,
        data,
      }}
    >
      <form
        ref={formRef}
        onSubmit={(e) => {
          e.preventDefault();
          void form.handleSubmit();
        }}
        onKeyDown={onKeyDown}
        className={formClassName}
      >
        {renderShell(regions)}
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
