"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchGroupMembers } from "@/lib/rs/group";
import { inactiveRefClass } from "@/lib/page-def/reference";

/** How many names the tooltip lists before the rest is only counted. */
const MAX_NAMES = 25;

/**
 * The members of a group, as the tooltip of its pill (see [EntityChip]) — who a calendar's access
 * list or a poll actually reaches.
 *
 * Rendered as the tooltip's content, so it mounts only while the tooltip is open: the members are
 * fetched on the first hover, not for every pill of a form. Only who may see the group gets its
 * members (`GroupEntityRest.members`); anybody else is told so.
 */
export function GroupMembersHint({ groupId }: { groupId: number }) {
  const t = useTranslations();
  const { data, isPending, isError } = useQuery({
    queryKey: ["group", "members", groupId],
    queryFn: ({ signal }) => fetchGroupMembers(groupId, signal),
    staleTime: 60_000,
  });
  if (isPending) return <span>{t("group.members.tooltip.loading")}</span>;
  if (isError || !data.visible) {
    return <span>{t("group.members.tooltip.notVisible")}</span>;
  }
  const { members } = data;
  if (members.length === 0)
    return <span>{t("group.members.tooltip.none")}</span>;
  const rest = members.length - MAX_NAMES;
  return (
    <div className="flex flex-col">
      <span className="font-semibold">
        {t("group.members.tooltip.count", { arg0: members.length })}
      </span>
      {members.slice(0, MAX_NAMES).map((member) => (
        <span key={member.id} className={inactiveRefClass(member)}>
          {member.displayName}
        </span>
      ))}
      {rest > 0 && <span>{t("select.checklist.andMore", { arg0: rest })}</span>}
    </div>
  );
}
