"use client";

import { useRouter } from "next/navigation";
import type { EditOutcome } from "@/components/shared/edit/edit-outcome";
import {
  MERLIN_ROUTE,
  merlinEditRoute,
  merlinExecuteRoute,
} from "./merlin-routes";

/**
 * Where saving a template leads, as the legacy page's `afterOperationRedirectTo`: a new template stays in its
 * form, now stored, for the upload of its Word® template; a changed one goes to its execution.
 */
export function useMerlinEditOutcome(isNew: boolean): Partial<EditOutcome> {
  const router = useRouter();
  return {
    afterSave: (savedId) => {
      if (savedId == null) router.push(MERLIN_ROUTE);
      else
        router.push(
          isNew ? merlinEditRoute(savedId) : merlinExecuteRoute(savedId)
        );
    },
  };
}
