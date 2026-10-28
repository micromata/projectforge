"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DescribedMenuItem } from "@/components/shared/described-menu-item";
import { useElapsedSeconds } from "@/hooks/use-elapsed-seconds";
import { toast } from "@/lib/toast";
import { PropertiesDiffDialog } from "./properties-editor/properties-diff-dialog";
import { PropertiesEditorDialog } from "./properties-editor/properties-editor-dialog";
import { systemActionMenus, type SystemAction } from "./system-actions";

/**
 * The one-click administration actions of the System page as a row of menus, one per topic (see
 * [systemActionMenus]); the configuration menu also opens the properties editor and the comparison of
 * projectforge.properties with its backups. A message action
 * toasts its translated result, a download action lets the browser save the file and only toasts on
 * failure, a confirming action asks first. While an action runs (a download or sync may take a while, the
 * request reports no progress), a moving bar with its label and the seconds passed shows below the menus,
 * and the actions are disabled. The state lives here, not in the items, because a menu
 * unmounts its items as soon as it closes.
 */
export function SystemMenuBar({
  developmentMode,
  sipgateConfigured,
}: {
  developmentMode: boolean;
  sipgateConfigured: boolean;
}) {
  const t = useTranslations();
  const [confirmAction, setConfirmAction] = useState<SystemAction | null>(null);
  const [propertiesEditorOpen, setPropertiesEditorOpen] = useState(false);
  const [propertiesDiffOpen, setPropertiesDiffOpen] = useState(false);

  const mutation = useMutation({
    mutationFn: (action: SystemAction) => action.run(),
    onSuccess: (res) => {
      if (res && "message" in res && res.message) toast.success(res.message);
    },
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });

  function select(action: SystemAction) {
    if (action.confirmKey) setConfirmAction(action);
    else mutation.mutate(action);
  }

  return (
    <>
      <div className="flex flex-wrap gap-2">
        {systemActionMenus(developmentMode, sipgateConfigured).map((menu) => (
          <DropdownMenu key={menu.id}>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="gap-1">
                {t(menu.titleKey)}
                <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-80">
              {menu.id === "configuration" && (
                <>
                  <DescribedMenuItem
                    label={t("system.propertiesEditor.title")}
                    description={t("system.propertiesEditor.cardInfo")}
                    onSelect={() => setPropertiesEditorOpen(true)}
                  />
                  <DescribedMenuItem
                    label={t("system.propertiesEditor.diff.title")}
                    description={t("system.propertiesEditor.diff.info")}
                    onSelect={() => setPropertiesDiffOpen(true)}
                  />
                  <DropdownMenuSeparator />
                </>
              )}
              {menu.actions.map((action) => (
                <DescribedMenuItem
                  key={action.key}
                  label={t(action.labelKey)}
                  description={
                    action.tooltipKey ? t(action.tooltipKey) : undefined
                  }
                  disabled={mutation.isPending}
                  onSelect={() => select(action)}
                />
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        ))}
      </div>
      {mutation.isPending && mutation.variables && (
        <RunningAction label={t(mutation.variables.labelKey)} />
      )}
      {confirmAction && (
        <ConfirmDialog
          open
          onOpenChange={(open) => !open && setConfirmAction(null)}
          title={t(confirmAction.labelKey)}
          description={t(
            confirmAction.confirmKey ?? "",
            confirmAction.confirmValues
          )}
          confirmLabel={t(confirmAction.labelKey)}
          onConfirm={() => mutation.mutate(confirmAction)}
        />
      )}
      {propertiesEditorOpen && (
        <PropertiesEditorDialog
          onClose={() => setPropertiesEditorOpen(false)}
        />
      )}
      {propertiesDiffOpen && (
        <PropertiesDiffDialog onClose={() => setPropertiesDiffOpen(false)} />
      )}
    </>
  );
}

/** The running action: its label, the seconds passed and the moving bar of the uploads (not a percentage). */
function RunningAction({ label }: { label: string }) {
  const elapsed = useElapsedSeconds(true);
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex max-w-md flex-col gap-2 pt-2"
    >
      <p className="text-sm">
        {label}
        {elapsed > 0 && (
          <span className="text-muted-foreground"> · {elapsed} s</span>
        )}
      </p>
      {/* upload-progress styles the fill from the outside (globals.css): moving stripes. */}
      <Progress
        value={100}
        aria-label={label}
        className="upload-progress upload-progress-active h-2"
      />
    </div>
  );
}
