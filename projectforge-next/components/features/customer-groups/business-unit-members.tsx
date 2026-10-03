"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { FreeTextBadge } from "@/components/shared/free-text-badge";
import { GroupBadge } from "@/components/shared/group-badge";
import type { BusinessUnitMember } from "@/lib/rs/customer-groups";
import { cn } from "@/lib/utils";

/**
 * What a business unit stands for in the last five years' orders and invoices — its groups, and the
 * customers and free texts in no group — as the server evaluates the unsaved values, so a customer a
 * group, a name pattern or a task brings in is seen in the row. Undefined while the first answer is
 * pending: nothing is shown rather than "none".
 */
export function BusinessUnitMembers({
  members,
}: {
  members: BusinessUnitMember[] | undefined;
}) {
  const t = useTranslations();
  if (!members) return null;
  return (
    <div className="flex flex-col gap-1 text-xs md:col-span-2">
      <h3 className="font-semibold">
        {t("fibu.businessUnits.members", { arg0: members.length })}
      </h3>
      <p className="text-muted-foreground">
        {t("fibu.businessUnits.membersHint")}
      </p>
      {members.length === 0 ? (
        <p className="text-muted-foreground">
          {t("fibu.businessUnits.membersNone")}
        </p>
      ) : (
        <ul className="grid gap-x-4 sm:grid-cols-2">
          {members.map((member) => (
            <MemberItem key={`${member.kind}:${member.name}`} member={member} />
          ))}
        </ul>
      )}
    </div>
  );
}

function MemberItem({ member }: { member: BusinessUnitMember }) {
  const t = useTranslations();
  return (
    <li
      className={cn(
        "flex min-w-0 items-center gap-1.5",
        // Italic, as the free-text customers are set apart in the checklists too.
        member.kind === "FREE_TEXT" && "italic"
      )}
    >
      <span className="truncate">{member.name}</span>
      {member.kind === "GROUP" && <GroupBadge />}
      {member.kind === "FREE_TEXT" && <FreeTextBadge />}
      {member.viaTask && (
        <Badge
          variant="outline"
          className="h-4 shrink-0 px-1 text-[10px] font-medium not-italic text-muted-foreground"
        >
          {t("fibu.businessUnits.viaTask")}
        </Badge>
      )}
    </li>
  );
}
