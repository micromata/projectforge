"use client";

import type { CSSProperties, ReactNode, RefObject } from "react";
import { useTranslations } from "next-intl";
import { useDraggable } from "@dnd-kit/core";
import { CSS } from "@dnd-kit/utilities";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  Cancel01Icon,
  DragDropVerticalIcon,
  SquareArrowUpRightIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { BASE_PATH } from "@/lib/config";
import { cn } from "@/lib/utils";
import {
  useCostSearchPanelStore,
  type PanelPosition,
} from "@/store/cost-search-panel-store";

/** The pop-out window's name: a second click brings the same window back instead of opening another. */
const POPOUT_WINDOW = "pfCostSearch";

interface Props {
  title: string;
  /** Null until first moved: then it sits in the bottom right corner, above the status bar. */
  position: PanelPosition | null;
  draggable: boolean;
  collapsed: boolean;
  nodeRef: RefObject<HTMLElement | null>;
  children: ReactNode;
}

/** The panel's card: the title bar (drag handle and buttons) and, unless collapsed, its content. */
export function CostSearchPanelFrame({
  title,
  position,
  draggable,
  collapsed,
  nodeRef,
  children,
}: Props) {
  const t = useTranslations();
  const setCollapsed = useCostSearchPanelStore((s) => s.setCollapsed);
  const close = useCostSearchPanelStore((s) => s.close);
  const { setNodeRef, setActivatorNodeRef, listeners, attributes, transform } =
    useDraggable({ id: "cost-search-panel", disabled: !draggable });

  function popOut() {
    window.open(
      `${BASE_PATH}/costSearch/popout`,
      POPOUT_WINDOW,
      "popup,width=520,height=440"
    );
    // It moved into its own window; both at once would only be two places to look.
    close();
  }

  // Inline only what is dynamic: the stored position (as CSS variables, so the phone layout's classes
  // win below `md`) and the drag offset.
  const style = {
    ...(position && {
      "--panel-x": `${position.x}px`,
      "--panel-y": `${position.y}px`,
    }),
    transform: CSS.Translate.toString(transform),
  } as CSSProperties;

  return (
    <section
      ref={(node) => {
        setNodeRef(node);
        nodeRef.current = node;
      }}
      style={style}
      aria-label={title}
      data-testid="cost-search-panel"
      className={cn(
        "fixed inset-x-2 bottom-2 z-40 flex flex-col rounded-lg border bg-background shadow-lg print:hidden",
        "md:inset-x-auto md:w-96",
        position
          ? "md:bottom-auto md:left-(--panel-x) md:top-(--panel-y)"
          : "md:right-4 md:bottom-12"
      )}
    >
      <div className="flex items-center gap-1 border-b py-1 pr-1 pl-2">
        <div
          ref={setActivatorNodeRef}
          {...(draggable ? { ...listeners, ...attributes } : {})}
          aria-label={draggable ? title : undefined}
          className={cn(
            "flex min-w-0 flex-1 items-center gap-1.5 text-sm font-semibold",
            draggable && "cursor-grab touch-none active:cursor-grabbing"
          )}
        >
          {draggable && (
            <HugeiconsIcon
              icon={DragDropVerticalIcon}
              size={16}
              className="shrink-0 text-muted-foreground"
            />
          )}
          <span className="truncate">{title}</span>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={t("fibu.kostSearch.popOut")}
          onClick={popOut}
          className="hidden md:inline-flex"
        >
          <HugeiconsIcon icon={SquareArrowUpRightIcon} size={16} />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={collapsed ? t("expand") : t("collapse")}
          aria-expanded={!collapsed}
          onClick={() => setCollapsed(!collapsed)}
        >
          <HugeiconsIcon
            icon={collapsed ? ArrowUp01Icon : ArrowDown01Icon}
            size={16}
          />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={t("close")}
          onClick={close}
        >
          <HugeiconsIcon icon={Cancel01Icon} size={16} />
        </Button>
      </div>
      {/* Hidden rather than unmounted: the units looked up last are still there when expanded again. */}
      <div className={cn("p-3", collapsed && "hidden")}>{children}</div>
    </section>
  );
}
