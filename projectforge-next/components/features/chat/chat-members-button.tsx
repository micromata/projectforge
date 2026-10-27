"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserMultiple02Icon } from "@hugeicons/core-free-icons";
import { RefName } from "@/components/shared/ref-list";
import { Spinner } from "@/components/shared/spinner";
import { UserAvatar } from "@/components/shared/user-avatar";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTitle,
  PopoverTrigger,
} from "@/components/ui/popover";
import { fetchChatMembers } from "@/lib/rs/chat";

/**
 * The members of a conversation (room, data transfer area, direct message) with their avatars, in a popover: the
 * participants of the host who may read it, loaded when opened.
 */
export function ChatMembersButton({
  conversationId,
  meId,
}: {
  conversationId: number;
  meId: number;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const query = useQuery({
    queryKey: ["chatMembers", conversationId],
    queryFn: ({ signal }) => fetchChatMembers(conversationId, signal),
    enabled: open,
    refetchOnWindowFocus: false,
  });
  const members = query.data ?? [];
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          aria-label={t("chat.conversation.members")}
        >
          <HugeiconsIcon icon={UserMultiple02Icon} />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="gap-2">
        <PopoverTitle>
          {query.data
            ? t("chat.conversation.membersCount", { arg0: members.length })
            : t("chat.conversation.members")}
        </PopoverTitle>
        {query.isPending && (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        )}
        <ul className="-mx-1 max-h-80 overflow-y-auto">
          {members.map((member) => (
            <li
              key={member.id}
              className="flex items-center gap-2 rounded-sm px-1 py-1"
            >
              <UserAvatar
                name={member.displayName}
                flags={member}
                userId={member.id}
                avatarVersion={member.avatarVersion}
              />
              <span className="min-w-0 flex-1 truncate text-sm">
                <RefName value={member} />
                {member.id === meId && (
                  <span className="text-muted-foreground">
                    {" "}
                    {t("chat.conversation.membersMe")}
                  </span>
                )}
              </span>
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
