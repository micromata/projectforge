"use client";

import { createContext, useCallback, useContext, useState } from "react";
import type { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { callAction as callActionApi } from "@/lib/rs/client";
import type {
  ActionDef,
  DynamicUIResponse,
  ValidationError,
} from "@/lib/rs/types";

export interface DynamicLayoutContextValue {
  data: Record<string, unknown>;
  ui: DynamicUIResponse;
  variables: Record<string, unknown>;
  validationErrors: ValidationError[];
  isFetching: boolean;
  setData: (patch: Record<string, unknown>) => void;
  setVariables: (patch: Record<string, unknown>) => void;
  callAction: (action: ActionDef) => Promise<void>;
  translate: (key: string) => string;
}

const DynamicLayoutContext = createContext<DynamicLayoutContextValue | null>(
  null
);

export function useDynamicLayout() {
  const ctx = useContext(DynamicLayoutContext);
  if (!ctx) throw new Error("useDynamicLayout must be inside DynamicLayoutProvider");
  return ctx;
}

interface ProviderProps {
  ui: DynamicUIResponse;
  initialData: Record<string, unknown>;
  initialVariables?: Record<string, unknown>;
  initialValidationErrors?: ValidationError[];
  children: ReactNode;
  onUpdate?: (response: {
    data: Record<string, unknown>;
    ui?: DynamicUIResponse;
    variables?: Record<string, unknown>;
    validationErrors?: ValidationError[];
  }) => void;
}

export function DynamicLayoutProvider({
  ui,
  initialData,
  initialVariables,
  initialValidationErrors,
  children,
  onUpdate,
}: ProviderProps) {
  const router = useRouter();
  const [data, setDataState] = useState<Record<string, unknown>>(initialData);
  const [variables, setVariablesState] = useState<Record<string, unknown>>(
    initialVariables ?? {}
  );
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(
    initialValidationErrors ?? []
  );
  const [isFetching, setIsFetching] = useState(false);

  const setData = useCallback((patch: Record<string, unknown>) => {
    setDataState((prev) => ({ ...prev, ...patch }));
  }, []);

  const setVariables = useCallback((patch: Record<string, unknown>) => {
    setVariablesState((prev) => ({ ...prev, ...patch }));
  }, []);

  const translate = useCallback(
    (key: string) => ui.translations?.[key] ?? key,
    [ui.translations]
  );

  const handleAction = useCallback(
    async (action: ActionDef) => {
      if (!action.url) return;

      if (action.confirmMessage && !window.confirm(action.confirmMessage)) {
        return;
      }

      setIsFetching(true);
      try {
        const response = await callActionApi(action.url, data);
        setValidationErrors(response.validationErrors ?? []);

        const targetType = response.targetType ?? action.responseAction?.targetType;

        switch (targetType) {
          case "REDIRECT":
          case "CHECK_AUTHENTICATION": {
            const url = response.url ?? action.responseAction?.url;
            if (url) router.push(url);
            break;
          }
          case "UPDATE":
            setDataState(response.data ?? {});
            if (response.variables) setVariablesState(response.variables);
            onUpdate?.(response);
            break;
          case "CLOSE_MODAL":
          case "CANCEL":
            router.back();
            break;
          case "TOAST": {
            const msg = response.url ?? action.responseAction?.message ?? "";
            toast.success(msg);
            break;
          }
          case "DOWNLOAD": {
            const url = response.url ?? action.responseAction?.url;
            if (url) window.location.href = url;
            break;
          }
          case "NOTHING":
            break;
          default:
            if (response.url) router.push(response.url);
            break;
        }
      } catch (err) {
        toast.error(err instanceof Error ? err.message : "Action failed");
      } finally {
        setIsFetching(false);
      }
    },
    [data, router, onUpdate]
  );

  return (
    <DynamicLayoutContext.Provider
      value={{
        data,
        ui,
        variables,
        validationErrors,
        isFetching,
        setData,
        setVariables,
        callAction: handleAction,
        translate,
      }}
    >
      {children}
    </DynamicLayoutContext.Provider>
  );
}
