"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
} from "react";
import type { ReactNode } from "react";
import type { DataObject } from "@/lib/dynamic/path";
import type {
  ActionDef,
  DynamicPageResponse,
  DynamicUIResponse,
  ValidationError,
} from "@/lib/rs/types";
import { useDynamicActions } from "./use-dynamic-actions";
import { useDynamicLayoutState } from "./use-dynamic-layout";

export interface DynamicLayoutContextValue {
  data: DataObject;
  ui: DynamicUIResponse;
  variables: DataObject;
  validationErrors: ValidationError[];
  isFetching: boolean;
  setData: (patch: DataObject) => void;
  setVariables: (patch: DataObject) => void;
  callAction: (action: ActionDef) => Promise<void>;
  /** Looks up a label in `ui.translations`; falls back to the key so a gap stays visible. */
  translate: (key: string) => string;
}

/** Exported for DynamicList, which re-provides a scoped view of one list element. */
export const DynamicLayoutContext =
  createContext<DynamicLayoutContextValue | null>(null);

export function useDynamicLayout() {
  const ctx = useContext(DynamicLayoutContext);
  if (!ctx)
    throw new Error("useDynamicLayout must be inside DynamicLayoutProvider");
  return ctx;
}

interface ProviderProps {
  /** The server's answer, layout and data in one - see rest/dto/FormLayoutData.kt. */
  response: DynamicPageResponse;
  /** Rest category, needed for the `{category}/watchFields` endpoint. */
  category: string;
  /** Query key of the page's layout query, invalidated by a RELOAD action. */
  queryKey: readonly unknown[];
  children: ReactNode;
}

export function DynamicLayoutProvider({
  response,
  category,
  queryKey,
  children,
}: ProviderProps) {
  // The two hooks are mutually dependent: a data change may trigger a watchFields call, whose
  // answer is an action that changes the data again. The store is created first, and the action
  // hook is wired into it afterwards through this ref.
  const triggerRef = useRef<((triggered: string[]) => void) | null>(null);
  const trigger = useCallback(
    (triggered: string[]) => triggerRef.current?.(triggered),
    []
  );
  const store = useDynamicLayoutState(response, trigger);
  const { callAction, triggerWatchFields } = useDynamicActions(
    store,
    category,
    queryKey
  );
  useEffect(() => {
    triggerRef.current = triggerWatchFields;
  }, [triggerWatchFields]);

  const translate = useCallback(
    (key: string) => store.ui.translations?.[key] ?? key,
    [store.ui.translations]
  );

  return (
    <DynamicLayoutContext.Provider
      value={{
        data: store.data,
        ui: store.ui,
        variables: store.variables,
        validationErrors: store.validationErrors,
        isFetching: store.isFetching,
        setData: store.setData,
        setVariables: store.setVariables,
        callAction,
        translate,
      }}
    >
      {children}
    </DynamicLayoutContext.Provider>
  );
}
