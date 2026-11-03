"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { UserBlock01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import type { ChatRef } from "@/lib/rs/chat-types";
import { useBlockUserAction } from "./use-block-user-action";

/** Blocks or unblocks the other one of a direct message (DM-07), in the head of the conversation. */
export function DirectBlockButton({ partner }: { partner: ChatRef }) {
  const action = useBlockUserAction(partner);
  return (
    <>
      <HintTooltip text={action.label} plain>
        <Button
          variant="ghost"
          size="icon"
          aria-label={action.label}
          aria-pressed={action.blocked}
          disabled={action.pending}
          onClick={action.run}
        >
          <HugeiconsIcon icon={UserBlock01Icon} />
        </Button>
      </HintTooltip>
      {action.dialog}
    </>
  );
}
