"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import {
  acceptTaskBookingInvitation,
  fetchTaskBookingInvitation,
} from "@/lib/rs/task-booking-grant";
import { TASK_STRUCTURE_QUERY_KEYS } from "@/lib/rs/task";
import { timesheetAddRoute } from "@/lib/timesheet-links";

/**
 * The invitation link of an ad hoc booking grant (`?token=`): shows the task, the period and who granted it,
 * and accepts it. Only for the user of the grant: anybody else (a forwarded link), an expired or an already
 * used token get the same message and learn nothing of the grant — the backend's rule, not this page's.
 */
export function BookingInvitation() {
  const t = useTranslations();
  const ctx = useFormatContext();
  const queryClient = useQueryClient();
  const token = useSearchParams().get("token") ?? "";
  const invitation = useQuery({
    queryKey: ["taskBookingInvitation", token],
    queryFn: ({ signal }) => fetchTaskBookingInvitation(token, signal),
    enabled: token !== "",
    retry: false,
  });
  const accept = useMutation({
    mutationFn: () => acceptTaskBookingInvitation(token),
    // The tree and the task infos now show the granted task.
    onSuccess: (result) => {
      if (result.success) {
        TASK_STRUCTURE_QUERY_KEYS.forEach((queryKey) =>
          queryClient.invalidateQueries({ queryKey })
        );
      }
    },
  });

  if (accept.data?.success) {
    return (
      <div className="flex flex-col items-start gap-3">
        <FormAlert tone="success">
          {t("task.bookingGrant.invitation.accepted")}
        </FormAlert>
        {accept.data.taskId != null && (
          <Button asChild>
            <Link href={timesheetAddRoute(accept.data.taskId)}>
              {t("task.bookingGrant.invitation.bookNow")}
            </Link>
          </Button>
        )}
      </div>
    );
  }
  if (token !== "" && invitation.isPending) return <Spinner />;
  const data = invitation.data;
  if (!data) {
    return (
      <FormAlert tone="error">
        {t("task.bookingGrant.error.invitationInvalid")}
      </FormAlert>
    );
  }

  return (
    <div className="flex max-w-2xl flex-col gap-4">
      <p>{t("task.bookingGrant.invitation.text")}</p>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
        <dt className="font-semibold">{t("task._")}</dt>
        <dd>{data.taskPath}</dd>
        <dt className="font-semibold">{t("task.bookingGrant.validFrom")}</dt>
        <dd>{formatDate(data.validFrom, ctx)}</dd>
        <dt className="font-semibold">{t("task.bookingGrant.validUntil")}</dt>
        <dd>{formatDate(data.validUntil, ctx)}</dd>
        <dt className="font-semibold">{t("task.bookingGrant.grantedBy")}</dt>
        <dd>{data.grantedByName}</dd>
        {data.comment && (
          <>
            <dt className="font-semibold">{t("comment")}</dt>
            <dd className="whitespace-pre-line">{data.comment}</dd>
          </>
        )}
      </dl>
      {accept.data && !accept.data.success && (
        <FormAlert tone="error">{accept.data.message}</FormAlert>
      )}
      {accept.isError && (
        <FormAlert tone="error">{t("errorpage.title")}</FormAlert>
      )}
      <div>
        <Button
          type="button"
          disabled={accept.isPending}
          onClick={() => accept.mutate()}
        >
          {accept.isPending && <Spinner className="h-4 w-4 border-2" />}
          {t("task.bookingGrant.invitation.accept")}
        </Button>
      </div>
    </div>
  );
}
