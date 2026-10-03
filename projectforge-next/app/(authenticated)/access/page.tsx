"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ACCESS_PAGE } from "@/components/features/access/access.page";
import {
  ACCESS_TASK_ID_PARAM,
  ACCESS_TASK_NAME_PARAM,
} from "@/lib/access-links";
import type { MagicFilter } from "@/lib/rs/types";

export default function AccessListPage() {
  return (
    // `useSearchParams` needs this boundary under `output: "export"`; the first, empty read is just the
    // list with its remembered filter (no task jump).
    <Suspense fallback={<EntityListPage page={ACCESS_PAGE} />}>
      <AccessListBody />
    </Suspense>
  );
}

/**
 * The access list, optionally opened by a task form's "show access rights" jump (`?taskId=…&taskName=…`,
 * see lib/access-links.ts), which seeds a **transient** filter on that structure element — not merged with
 * the remembered filter and not stored back afterwards.
 */
function AccessListBody() {
  const params = useSearchParams();
  const taskId = Number(params.get(ACCESS_TASK_ID_PARAM));
  const taskName = params.get(ACCESS_TASK_NAME_PARAM) ?? undefined;

  // The backend's `task` filter reads `value.id` (GroupAccessEntityRest.preProcessMagicFilter); the name
  // is for the filter pill.
  const filterOverride: MagicFilter | undefined =
    taskId > 0
      ? {
          entries: [
            { field: "task", value: { id: taskId, displayName: taskName } },
          ],
          sortProperties: [],
        }
      : undefined;

  return (
    <EntityListPage
      page={ACCESS_PAGE}
      filterOverride={filterOverride}
      transient={!!filterOverride}
    />
  );
}
