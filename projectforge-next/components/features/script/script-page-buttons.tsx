"use client";

import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { SourceCodeIcon } from "@hugeicons/core-free-icons";
import { GuardedLink } from "@/components/shared/guarded-link";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { RichText } from "@/components/shared/rich-text";
import { Button } from "@/components/ui/button";
import {
  fetchScriptPageButtons,
  scriptPageButtonsQueryKey,
} from "@/lib/rs/script";
import { scriptPageButtonRoute } from "./script-routes";

/** The buttons rarely change: a script's configuration only. Saving a script refetches them anyway. */
const STALE_MS = 5 * 60 * 1000;

/**
 * The buttons of the scripts configured for this page (`ScriptDO.pageTargets`) that the user may execute,
 * for the top right corner of the page. A button opens the script's execution page, where the script gets
 * the page's current filter (the backend keeps it, see `ScriptPageTargets`). Mostly there is none, and
 * nothing is rendered.
 *
 * @param target The page target, e.g. `list:order` or `orderStatistics:forecast`.
 */
export function ScriptPageButtons({ target }: { target: string }) {
  const buttons = useQuery({
    queryKey: scriptPageButtonsQueryKey(target),
    queryFn: ({ signal }) => fetchScriptPageButtons(target, signal),
    staleTime: STALE_MS,
    // A missing right or an error must not disturb the page the buttons are only an addition to.
    retry: false,
  });
  return buttons.data?.map((button) => (
    <HintTooltip
      key={button.id}
      // The tooltip falls back to the script's description, which is rich text (HTML); RichText also
      // takes the plain text or markdown of an own tooltip.
      content={button.tooltip ? <RichText html={button.tooltip} /> : undefined}
    >
      <Button asChild variant="ghost" size="sm" className="gap-1.5">
        <GuardedLink href={scriptPageButtonRoute(button.id, target)}>
          <HugeiconsIcon icon={SourceCodeIcon} size={14} aria-hidden />
          {button.label}
        </GuardedLink>
      </Button>
    </HintTooltip>
  ));
}
