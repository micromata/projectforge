"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useMenu } from "@/hooks/use-menu";
import {
  fetchDefaultMenu,
  resetCustomMenu,
  saveCustomMenu,
} from "@/lib/rs/menu-customizer";
import type { MenuItem } from "@/lib/rs/types";
import { toast } from "@/lib/toast";
import { cleanForSave } from "./menu-tree";

/**
 * The state of the customizer page: the stored menu (from the shared `["menu"]` query, which the favorites
 * bar renders too) and the user's unsaved draft on top of it.
 *
 * No draft means "as stored", so after a save or a reset the draft is simply dropped once the menu query
 * has been refetched — the page then shows what the server made of it (the ids of new groups are the
 * server's), and the favorites bar shows the same without a reload.
 */
export function useMenuCustomizer() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const menuQuery = useMenu();
  const [draft, setDraft] = useState<MenuItem[] | null>(null);

  const stored = menuQuery.data?.favoritesMenu?.menuItems ?? [];
  const menu = draft ?? stored;

  /** Applies [change] to the current menu; a change returning the same list leaves the page as it is. */
  const update = (change: (menu: MenuItem[]) => MenuItem[]) =>
    setDraft((prev) => {
      const current = prev ?? stored;
      const next = change(current);
      return next === current ? prev : next;
    });

  const reloadStored = async () => {
    await queryClient.invalidateQueries({ queryKey: ["menu"] });
    setDraft(null);
  };

  const save = useMutation({
    mutationFn: () => saveCustomMenu(cleanForSave(menu)),
    onSuccess: async () => {
      await reloadStored();
      toast.success(t("menu.customizer.menuSavedSuccessfully"));
    },
    onError: () => toast.error(t("menu.customizer.errorSavingMenu")),
  });

  const reset = useMutation({
    mutationFn: resetCustomMenu,
    onSuccess: async () => {
      await reloadStored();
      toast.success(t("menu.customizer.menuResetSuccessfully"));
    },
    onError: () => toast.error(t("menu.customizer.errorResettingMenu")),
  });

  const loadDefault = useMutation({
    mutationFn: () => fetchDefaultMenu(),
    onSuccess: (items) => setDraft(items),
    onError: () => toast.error(t("menu.customizer.errorLoadingMenu")),
  });

  return {
    menuQuery,
    /** The main menu, grouped by its categories: what can be added. */
    template: menuQuery.data?.mainMenu?.menuItems ?? [],
    menu,
    dirty: draft !== null,
    busy: save.isPending || reset.isPending || loadDefault.isPending,
    update,
    undo: () => setDraft(null),
    save: () => save.mutate(),
    reset: () => reset.mutate(),
    loadDefault: () => loadDefault.mutate(),
  };
}
