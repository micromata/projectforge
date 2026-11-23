"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { Table } from "@tanstack/react-table";
import type { MagicFilter } from "@/lib/rs/types";
import { useCopyTable } from "./use-copy-table";

interface CopyTableContextValue {
  copy: () => Promise<void>;
  isPending: boolean;
  /** A list's own export menu announces itself, so the page adds none of its own (see CopyOnlyExportMenu). */
  hasExportMenu: boolean;
  registerExportMenu: () => () => void;
}

const CopyTableContext = createContext<CopyTableContextValue | null>(null);

/**
 * Offers "copy the table" to the export menus of a list's toolbar (see ExportMenu), which carry it as an
 * entry. Outside a list there is no provider and an export menu stays what it was.
 */
export function CopyTableProvider<Row>({
  table,
  entity,
  filter,
  children,
}: {
  table: Table<Row>;
  entity: string;
  filter: MagicFilter;
  children: ReactNode;
}) {
  const { copy, isPending } = useCopyTable({ table, entity, filter });
  const [menus, setMenus] = useState(0);
  const registerExportMenu = useCallback(() => {
    setMenus((n) => n + 1);
    return () => setMenus((n) => n - 1);
  }, []);
  const value = useMemo(
    () => ({
      copy,
      isPending,
      hasExportMenu: menus > 0,
      registerExportMenu,
    }),
    [copy, isPending, menus, registerExportMenu]
  );
  return (
    <CopyTableContext.Provider value={value}>
      {children}
    </CopyTableContext.Provider>
  );
}

/** Null outside a list. */
export function useCopyTableContext(): CopyTableContextValue | null {
  return useContext(CopyTableContext);
}

/** Registers the calling export menu with the list it sits in, if any. */
export function useRegisterExportMenu(enabled: boolean) {
  const register = useCopyTableContext()?.registerExportMenu;
  useEffect(() => (enabled ? register?.() : undefined), [enabled, register]);
}
