"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { FormAlert } from "@/components/shared/form-alert";
import { confirmLeaveUnsavedChanges } from "@/hooks/use-unsaved-changes-warning";
import { PropertiesEditorToolbar } from "./properties-editor-toolbar";
import { PropertyGroupSection } from "./property-group-section";
import { usePropertiesEditor } from "./use-properties-editor";

/**
 * The properties editor: all known parameters of projectforge.properties grouped by topic, with
 * their descriptions from application.properties. Saving writes a timestamped backup first; changes
 * take effect only after a restart, which the dialog says permanently and the save toast repeats.
 */
export function PropertiesEditorDialog({ onClose }: { onClose: () => void }) {
  const t = useTranslations();
  const editor = usePropertiesEditor();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const data = editor.query.data;
  const readOnly = !data?.writable;
  const searching =
    editor.filter.query.trim() !== "" ||
    editor.filter.onlyInFile ||
    editor.filter.onlyModified;

  async function requestClose() {
    if (await confirmLeaveUnsavedChanges()) onClose();
  }

  return (
    <Dialog open onOpenChange={(open) => !open && void requestClose()}>
      <DialogContent className="flex h-[90vh] flex-col gap-0 overflow-hidden p-0 sm:max-w-[min(96vw,88rem)]">
        <DialogHeader className="shrink-0 gap-2 px-6 pt-5 pb-3">
          <DialogTitle>{t("system.propertiesEditor.title")}</DialogTitle>
          <DialogDescription className="break-all font-mono text-xs">
            {data?.filePath}
          </DialogDescription>
          <FormAlert tone="info">
            <p className="font-medium">
              {t("system.propertiesEditor.restartRequired")}
            </p>
            <p>{t("system.propertiesEditor.backupInfo")}</p>
          </FormAlert>
          {data && readOnly && (
            <FormAlert tone="error">
              {t("system.propertiesEditor.notWritable")}
            </FormAlert>
          )}
          <PropertiesEditorToolbar
            filter={editor.filter}
            onFilterChange={editor.setFilter}
            onAdd={editor.addKey}
            disabled={readOnly}
          />
        </DialogHeader>
        <div className="flex-1 overflow-y-auto border-t px-6 py-3">
          {editor.query.isPending && (
            <p className="text-sm text-muted-foreground">{t("loading")}</p>
          )}
          {editor.query.isError && (
            <p className="text-sm text-destructive">
              {t("access.exception.noAccess")}
            </p>
          )}
          {data && editor.visibleGroups.length === 0 && (
            <p className="text-sm text-muted-foreground">
              {t("system.propertiesEditor.noResults")}
            </p>
          )}
          <div className="flex flex-col gap-2">
            {editor.visibleGroups.map((group) => (
              <PropertyGroupSection
                key={group.id}
                group={group}
                forceOpen={searching}
                edits={editor.edits}
                onEdit={editor.setEdit}
                query={editor.filter.query}
                readOnly={readOnly}
              />
            ))}
          </div>
        </div>
        <FormActionBar>
          <Button variant="outline" onClick={() => void requestClose()}>
            {t("cancel")}
          </Button>
          <Button
            disabled={readOnly || editor.changes.length === 0 || editor.saving}
            onClick={() => setConfirmOpen(true)}
          >
            {t("save")}
          </Button>
          {editor.changes.length > 0 && (
            <span className="text-sm text-muted-foreground">
              {t("system.propertiesEditor.changeCount", {
                arg0: editor.changes.length,
              })}
            </span>
          )}
        </FormActionBar>
        <ConfirmDialog
          open={confirmOpen}
          onOpenChange={setConfirmOpen}
          title={t("system.propertiesEditor.confirm.title")}
          description={
            <span className="flex flex-col gap-2">
              <span>{t("system.propertiesEditor.confirm.description")}</span>
              <span className="flex flex-col font-mono text-xs">
                {editor.changes.map((change) => (
                  <span key={change.key} className="break-all">
                    {change.remove
                      ? `${change.key}: ${t("system.propertiesEditor.removed")}`
                      : change.key}
                  </span>
                ))}
              </span>
              <span className="font-medium">
                {t("system.propertiesEditor.restartRequired")}
              </span>
            </span>
          }
          confirmLabel={t("save")}
          onConfirm={() => {
            setConfirmOpen(false);
            editor.save();
          }}
        />
      </DialogContent>
    </Dialog>
  );
}
