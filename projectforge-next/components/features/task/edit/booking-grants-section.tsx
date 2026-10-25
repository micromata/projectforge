"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { FormAlert } from "@/components/shared/form-alert";
import {
  confirmTaskBookingGrant,
  fetchTaskBookingGrants,
  revokeTaskBookingGrant,
  type TaskBookingGrant,
  type TaskBookingGrantResult,
} from "@/lib/rs/task-booking-grant";
import { TASK_STRUCTURE_QUERY_KEYS } from "@/lib/rs/task";
import { BookingGrantDialog } from "./booking-grant-dialog";
import { BookingGrantsTable } from "./booking-grants-table";

/**
 * The ad hoc booking grants of a task and its sub elements: who may book own time sheets on them for a period
 * without being member of a group, granted and revoked here by who may edit the time sheets of others on them.
 * Who may only book on them proposes grants instead (`mayPropose`): the time sheets are to be accepted by the
 * project management, which may confirm the grant, so later ones aren't anymore.
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
  const [confirming, setConfirming] = useState<TaskBookingGrant | null>(null);
  const [rejected, setRejected] = useState<string | null>(null);

  if (id == null || !(info?.mayGrant || info?.mayPropose)) return null;
  const proposing = info.mayPropose === true;

  const apply = (result: TaskBookingGrantResult) => {
    if (result.info) queryClient.setQueryData(queryKey, result.info);
    setRejected(result.success ? null : (result.message ?? null));
    if (result.success) {
      // The badges of the structure tree (grants, time sheets to accept) are cached as well.
      for (const key of TASK_STRUCTURE_QUERY_KEYS)
        void queryClient.invalidateQueries({ queryKey: key });
    }
  };

  const run = async (call: () => Promise<TaskBookingGrantResult>) => {
    setRevoking(null);
    setConfirming(null);
    try {
      apply(await call());
    } catch {
      setRejected(t("errorpage.title"));
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        {t("task.bookingGrants.info")}
      </p>
      {proposing && (
        <FormAlert tone="info">
          {t("task.bookingGrant.proposal.info")}
        </FormAlert>
      )}
      {rejected && <FormAlert tone="error">{rejected}</FormAlert>}
      {info.grants && info.grants.length > 0 ? (
        <BookingGrantsTable
          grants={info.grants}
          taskId={id}
          onConfirm={setConfirming}
          onRevoke={setRevoking}
        />
      ) : (
        <p className="text-sm">{t("task.bookingGrant.none")}</p>
      )}
      {info.tasks && info.tasks.length > 0 ? (
        <div>
          <Button
            type="button"
            variant="outline"
            onClick={() => setAdding(true)}
          >
            <HugeiconsIcon icon={PlusSignIcon} size={16} />
            {t(
              proposing ? "task.bookingGrant.propose" : "task.bookingGrant.add"
            )}
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
          info={info}
          onClose={() => setAdding(false)}
          onDone={(result) => {
            setAdding(false);
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
        onConfirm={() =>
          revoking && void run(() => revokeTaskBookingGrant(revoking.id, id))
        }
      />
      <ConfirmDialog
        open={confirming != null}
        onOpenChange={(open) => !open && setConfirming(null)}
        title={t("task.bookingGrant.confirm._")}
        description={t("task.bookingGrant.confirm.confirm")}
        confirmLabel={t("task.bookingGrant.confirm._")}
        onConfirm={() =>
          confirming &&
          void run(() => confirmTaskBookingGrant(confirming.id, id))
        }
      />
    </div>
  );
}
