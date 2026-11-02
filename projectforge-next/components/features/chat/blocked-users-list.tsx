"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { useBlockedUsers } from "./use-blocked-users";

/** The users the logged-in user has blocked, each with a button to remove the block. */
export function BlockedUsersList() {
  const t = useTranslations();
  const { blocked, unblock } = useBlockedUsers();
  return (
    <section className="flex flex-col gap-1">
      <h3 className="text-sm font-medium">{t("chat.block.list._")}</h3>
      {blocked.length === 0 ? (
        <p className="text-xs text-muted-foreground">
          {t("chat.block.list.empty")}
        </p>
      ) : (
        <ul className="flex flex-col gap-1">
          {blocked.map((user) => (
            <li
              key={user.id}
              className="flex items-center justify-between gap-2 text-sm"
            >
              <span className="min-w-0 truncate">{user.displayName}</span>
              <Button
                variant="outline"
                size="sm"
                disabled={unblock.isPending}
                onClick={() => unblock.mutate(user.id)}
              >
                {t("chat.block.unblock")}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
