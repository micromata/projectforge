"use client";

import { useRouter } from "next/navigation";
import type { EditOutcome } from "@/components/shared/edit/edit-outcome";
import { SCRIPT_ROUTE, scriptExecuteRoute } from "./script-routes";

/**
 * Where saving a script leads: to its execution, which is what it was written for — except an include,
 * which is never executed on its own and goes back to the list (as the legacy page's
 * `afterOperationRedirectTo` did).
 */
export function useScriptEditOutcome(): Partial<EditOutcome> {
  const router = useRouter();
  return {
    afterSave: (savedId, values) => {
      const type = (values as { type?: string } | null)?.type;
      router.push(
        savedId != null && type !== "INCLUDE"
          ? scriptExecuteRoute(SCRIPT_ROUTE, savedId)
          : SCRIPT_ROUTE
      );
    },
  };
}
