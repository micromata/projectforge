"use client";

import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { RefName } from "@/components/shared/ref-list";
import { UserAvatar } from "@/components/shared/user-avatar";
import { useOpenDirect } from "@/hooks/use-open-direct";

/** Users found by the search without a direct message yet: a click starts it (the conversation is created). */
export function ChatDirectCandidates({
  users,
  highlight,
}: {
  users: EntityRef[];
  highlight?: string;
}) {
  const t = useTranslations();
  const openDirect = useOpenDirect();
  return (
    <ul className="flex max-w-3xl flex-col divide-y rounded-md border">
      {users.map((user) => (
        <li key={user.id}>
          <button
            type="button"
            disabled={openDirect.isPending}
            aria-label={`${t("chat.direct.new")}: ${user.displayName}`}
            className="flex w-full items-center gap-2.5 px-3 py-2 text-left hover:underline"
            onClick={() => openDirect.mutate(user.id)}
          >
            <UserAvatar name={user.displayName} flags={user} userId={user.id} />
            <RefName
              value={user}
              highlight={highlight}
              className="truncate font-semibold"
            />
          </button>
        </li>
      ))}
    </ul>
  );
}
