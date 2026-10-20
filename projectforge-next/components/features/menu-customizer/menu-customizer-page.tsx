"use client";

import { DndContext, DragOverlay } from "@dnd-kit/core";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { CustomMenuPanel } from "./custom-menu-panel";
import { insertItem } from "./menu-tree";
import { TemplatePanel } from "./template-panel";
import { FAVORITES } from "./types";
import { useMenuCustomizer } from "./use-menu-customizer";
import { useMenuDragDrop } from "./use-menu-drag-drop";

/**
 * "Customize my menu" (`/next/customizeMenu`), successor of the old React page: the user composes the
 * favorites menu (the bar at the top) from the entries of the main menu — by drag and drop or by click
 * — in their own order and groups. Nothing is stored before Save; Undo returns to the stored menu, "Load
 * default" offers the default as a draft.
 */
export function MenuCustomizerPage() {
  const t = useTranslations();
  const customizer = useMenuCustomizer();
  const { dragged, contextProps } = useMenuDragDrop(customizer.update);
  const { menuQuery, menu, dirty, busy } = customizer;

  return (
    <PageShell>
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto">
          <PageTitleRow title={t("menu.customizer.title")} />
          <p className="px-4 pt-1 text-sm text-muted-foreground">
            {t("menu.customizer.subtitle")}
          </p>
          <div className="px-4 pb-6 pt-3">
            {menuQuery.isPending && (
              <p className="text-sm text-muted-foreground">{t("loading")}</p>
            )}
            {menuQuery.isError && (
              <p className="text-sm text-destructive">
                {t("menu.customizer.errorLoadingMenu")}
              </p>
            )}
            {menuQuery.data && (
              <DndContext {...contextProps}>
                {/*
                 * From sm: on, stays in view while scrolling through the template, so there is always a
                 * drop target. On a phone it would cover most of the screen, and entries are tapped there.
                 */}
                <div className="z-10 -mx-4 bg-background px-4 pt-1 pb-4 sm:sticky sm:top-0 sm:max-h-[45vh] sm:overflow-y-auto">
                  <CustomMenuPanel menu={menu} update={customizer.update} />
                </div>
                <TemplatePanel
                  categories={customizer.template}
                  menu={menu}
                  onAdd={(item) =>
                    customizer.update((m) => insertItem(m, FAVORITES, item))
                  }
                />
                <DragOverlay>
                  {dragged && (
                    <div className="flex h-6 items-center rounded-md border bg-popover px-1.5 text-xs/relaxed font-medium shadow-lg">
                      {dragged.title}
                    </div>
                  )}
                </DragOverlay>
              </DndContext>
            )}
          </div>
        </div>
        {/* Wraps on a phone: three buttons are wider than its screen. */}
        <FormActionBar className="flex-wrap gap-2">
          <Button
            variant="outline"
            disabled={!dirty || busy}
            onClick={customizer.undo}
          >
            {t("menu.customizer.undo")}
          </Button>
          <Button disabled={!dirty || busy} onClick={customizer.save}>
            {t("menu.customizer.saveChanges")}
          </Button>
          <div className="flex-1" />
          <Button
            variant="outline"
            disabled={busy}
            onClick={customizer.loadDefault}
          >
            {t("menu.customizer.loadDefault")}
          </Button>
        </FormActionBar>
      </div>
    </PageShell>
  );
}
