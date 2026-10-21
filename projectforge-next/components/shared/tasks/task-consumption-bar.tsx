"use client";

import { useTranslations } from "next-intl";
import { renderCell, type CellSpec } from "@/components/data-table";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import type { TaskNode } from "@/lib/rs/task";

/** The bar is the table's cell renderer, reached with the literal spec it needs (see TaskConsumptionCell). */
const CONSUMPTION: CellSpec = { kind: "consumption" };

/**
 * What is already booked on a task, outside a table — the same bar the tree and the task list show, and
 * like there a link to the time sheets behind it (see ConsumptionCell). The figure is the tooltip, which
 * the table's delegated tooltip would show inside a table.
 *
 * Renders nothing while the task carries no consumption (`info/{id}` sends it, the tree's nodes too).
 */
export function TaskConsumptionBar({ task }: { task: TaskNode }) {
  const t = useTranslations();
  const format = useFormatContext();
  if (task.consumption == null) return null;
  return (
    <HintTooltip plain text={(task.consumption as { title?: string }).title}>
      <div className="w-full">
        {renderCell({
          spec: CONSUMPTION,
          value: task.consumption,
          row: task,
          ctx: format,
          // The cells name their keys at runtime, which next-intl's literal key type cannot express.
          t: t as unknown as (key: string) => string,
        })}
      </div>
    </HintTooltip>
  );
}
