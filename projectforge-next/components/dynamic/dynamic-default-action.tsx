"use client";

import type { ReactNode } from "react";
import { useDynamicLayout } from "./dynamic-context";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import type { ActionDef } from "@/lib/rs/types";

/**
 * Which of a server-laid-out page's actions Return triggers: the one the backend marked as the
 * default (`UIButton.default` — "There should be one default button in every form, used if the user
 * hits return").
 *
 * Exported so [DynamicActionGroup] names the shortcut on the same button.
 */
export function defaultActionOf(actions?: ActionDef[]): ActionDef | undefined {
  return actions?.find((action) => action.default && !action.disabled);
}

/**
 * The container of a server-laid-out page, listening for the submit shortcut.
 *
 * Such a page has no `<form>` — it posts through `callAction` — so there is nothing the browser would
 * submit and Return did nothing at all here. A component of its own rather than a hook in
 * [DynamicPage], because `useDynamicLayout` only works inside the provider the page renders.
 */
export function DynamicDefaultAction({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  const { ui, callAction, isFetching } = useDynamicLayout();
  const action = defaultActionOf(ui.actions);
  const onKeyDown = useSubmitShortcut(
    () => action && void callAction(action),
    Boolean(action) && !isFetching
  );
  return (
    <div className={className} onKeyDown={onKeyDown}>
      {children}
    </div>
  );
}
