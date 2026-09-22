"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { fetchTaskInfo } from "@/lib/rs/task";
import { TaskPath } from "./task-path";

interface TaskRootBreadcrumbProps {
  /** The node the tree is rooted at, `null` for the whole tree. */
  rootTaskId: number | null;
  /** Re-root the tree at a node, or `null` for the whole tree. */
  onNavigate: (id: number | null) => void;
  onBack: () => void;
  onForward: () => void;
  canGoBack: boolean;
  canGoForward: boolean;
}

/**
 * Above the tree in a re-rootable panel: the path from the whole tree down to the node the tree is
 * currently rooted at, plus a browser-like back/forward pair over the visited roots.
 *
 * The crumbs are the [TaskPath] the select field uses, only their click re-roots instead of selecting —
 * home shows the whole tree, an ancestor shows its subtree, the current root is the (inert) last one. The
 * root's ancestors come from its own `info/{id}` answer, shared by query key with the field's breadcrumb.
 */
export function TaskRootBreadcrumb({
  rootTaskId,
  onNavigate,
  onBack,
  onForward,
  canGoBack,
  canGoForward,
}: TaskRootBreadcrumbProps) {
  const t = useTranslations("taskReRoot");

  const { data: root } = useQuery({
    queryKey: ["taskInfo", rootTaskId],
    queryFn: ({ signal }) => fetchTaskInfo(rootTaskId!, signal),
    enabled: rootTaskId != null,
    // The path of a node changes only when the tree is restructured, which is rare and never here.
    staleTime: Infinity,
  });

  return (
    <div className="flex min-w-0 items-center gap-1">
      <HintTooltip text={t("back")}>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t("back")}
          disabled={!canGoBack}
          onClick={onBack}
          className="size-6 shrink-0"
        >
          <HugeiconsIcon icon={ArrowLeft01Icon} size={14} />
        </Button>
      </HintTooltip>
      <HintTooltip text={t("forward")}>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t("forward")}
          disabled={!canGoForward}
          onClick={onForward}
          className="size-6 shrink-0"
        >
          <HugeiconsIcon icon={ArrowRight01Icon} size={14} />
        </Button>
      </HintTooltip>
      <div className="min-w-0 flex-1">
        <TaskPath
          task={rootTaskId != null ? (root ?? null) : null}
          onSelect={(node) => onNavigate(node?.id ?? null)}
          label={t("label")}
          homeTooltip={t("wholeTree")}
          ancestorTooltip={t("ancestor")}
          showPlaceholder={false}
          highlightCurrent
        />
      </div>
    </div>
  );
}
