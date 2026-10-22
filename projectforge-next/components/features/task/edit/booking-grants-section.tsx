"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { CopyButton } from "@/components/shared/copy-button";
import { FormAlert } from "@/components/shared/form-alert";
import {
  fetchTaskBookingGrants,
  revokeTaskBookingGrant,
  type TaskBookingGrant,
  type TaskBookingGrantResult,
} from "@/lib/rs/task-booking-grant";
import { BookingGrantDialog } from "./booking-grant-dialog";
import { BookingGrantsTable } from "./booking-grants-table";

/**
 * The ad hoc booking grants of a task: who may book own time sheets on it for a period without being member of a
 * group, granted and revoked here by who may edit the time sheets of others on the task.
 *
 * Only shown where the task says so (`bookingGrantAccess`, see the page definition); the endpoints check it
 * themselves. Each write goes through its own endpoint, not with the task form, and answers the fresh list.
 */
export function BookingGrantsSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const queryKey = ["taskBookingGrants", id];
  const { data: info } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchTaskBookingGrants(id!, signal),
    enabled: id != null,
  });
  const [adding, setAdding] = useState(false);
  const [revoking, setRevoking] = useState<TaskBookingGrant | null>(null);
  const [invitationUrl, setInvitationUrl] = useState<string | null>(null);
  const [rejected, setRejected] = useState<string | null>(null);

  if (id == null || !info?.mayGrant) return null;

  const apply = (result: TaskBookingGrantResult) => {
    if (result.info) queryClient.setQueryData(queryKey, result.info);
    setRejected(result.success ? null : (result.message ?? null));
  };

  const revoke = async (grant: TaskBookingGrant) => {
    setRevoking(null);
    try {
      apply(await revokeTaskBookingGrant(grant.id));
    } catch {
      setRejected(t("errorpage.title"));
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        {t("task.bookingGrants.info")}
      </p>
      {rejected && <FormAlert tone="error">{rejected}</FormAlert>}
      {invitationUrl && (
        <FormAlert tone="info">
          <div className="flex flex-col gap-1">
            <span>{t("task.bookingGrant.invitationUrl")}</span>
            <div className="flex items-center gap-2">
              <code className="min-w-0 flex-1 break-all text-xs">
                {invitationUrl}
              </code>
              <CopyButton
                value={invitationUrl}
                label={t("task.bookingGrant.invitationUrl")}
              />
            </div>
          </div>
        </FormAlert>
      )}
      {info.grants && info.grants.length > 0 ? (
        <BookingGrantsTable grants={info.grants} onRevoke={setRevoking} />
      ) : (
        <p className="text-sm">{t("task.bookingGrant.none")}</p>
      )}
      {info.bookable ? (
        <div>
          <Button
            type="button"
            variant="outline"
            onClick={() => setAdding(true)}
          >
            <HugeiconsIcon icon={PlusSignIcon} size={16} />
            {t("task.bookingGrant.add")}
          </Button>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">
          {t("task.bookingGrant.error.taskNotBookable")}
        </p>
      )}
      {adding && (
        <BookingGrantDialog
          taskId={id}
          kost2List={info.kost2List ?? []}
          onClose={() => setAdding(false)}
          onDone={(result) => {
            setAdding(false);
            setInvitationUrl(result.invitationUrl ?? null);
            apply(result);
          }}
        />
      )}
      <ConfirmDialog
        open={revoking != null}
        onOpenChange={(open) => !open && setRevoking(null)}
        title={t("task.bookingGrant.revoke._")}
        description={t("task.bookingGrant.revoke.confirm")}
        confirmLabel={t("task.bookingGrant.revoke._")}
        destructive
        onConfirm={() => revoking && void revoke(revoking)}
      />
    </div>
  );
}
