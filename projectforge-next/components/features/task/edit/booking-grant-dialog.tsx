"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { DateInput } from "@/components/shared/date-input";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { plusDays } from "@/lib/date-period-math";
import {
  grantTaskBooking,
  type TaskBookingGrantInfo,
  type TaskBookingGrantResult,
} from "@/lib/rs/task-booking-grant";
import { todayOf } from "@/lib/user-zone";
import { BookingGrantField } from "./booking-grant-field";
import {
  BookingGrantPositions,
  type BookingGrantPositionValue,
} from "./booking-grant-positions";

/**
 * Grants users the booking of own time sheets: any number of users, the structure elements (the task and
 * any of its sub elements, each with its own cost units), the period and a comment. One grant per user,
 * active right away; each user is informed by mail. Who may only propose (`mayPropose`) gives the same, but
 * the time sheets are to be accepted by the project management.
 *
 * Written right away through its own endpoint, not with the task form around it. What is wrong is the
 * backend's to say: its message is shown above the fields.
 */
export function BookingGrantDialog({
  taskId,
  info,
  onDone,
  onClose,
}: {
  taskId: number;
  info: TaskBookingGrantInfo;
  onDone: (result: TaskBookingGrantResult) => void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const today = todayOf(ctx);
  const tasks = info.tasks ?? [];
  // Who may only propose: the backend checks the limit of 31 days, the dialog only says so.
  const proposing = info.mayPropose === true;
  const actionKey = proposing
    ? "task.bookingGrant.propose"
    : "task.bookingGrant.add";
  const [users, setUsers] = useState<EntityRef[]>([]);
  // The task of the form, if it may be booked on: what a grant is about in most cases.
  const [positions, setPositions] = useState<BookingGrantPositionValue[]>(() =>
    tasks.some((task) => task.taskId === taskId)
      ? [{ taskId, kost2Ids: [] }]
      : []
  );
  const [validFrom, setValidFrom] = useState<string | null>(today);
  const [validUntil, setValidUntil] = useState<string | null>(
    plusDays(today, 13)
  );
  const [comment, setComment] = useState("");
  const grant = useMutation({
    mutationFn: () =>
      grantTaskBooking({
        taskId,
        userIds: users.map((user) => user.id),
        positions,
        validFrom,
        validUntil,
        comment: comment.trim() || null,
      }),
    onSuccess: (result) => result.success && onDone(result),
  });
  // A refusal comes as a result with its message (see TaskBookingGrantRest), a failure of the call as an error.
  const rejected = grant.isError
    ? t("errorpage.title")
    : grant.data && !grant.data.success
      ? (grant.data.message ?? t("errorpage.title"))
      : null;

  return (
    // Keeps Return inside the dialog from saving the task form around it (see ValidityEntryDialog).
    <div className="contents" onKeyDown={(event) => event.stopPropagation()}>
      <Dialog open onOpenChange={(open) => !open && onClose()}>
        <DialogContent
          className="max-h-[90vh] overflow-y-auto sm:max-w-2xl"
          onOpenAutoFocus={(event) => event.preventDefault()}
        >
          <DialogHeader>
            <DialogTitle>{t(actionKey)}</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            {proposing && (
              <FormAlert tone="info">
                {t("task.bookingGrant.proposal.info")}
              </FormAlert>
            )}
            {rejected && <FormAlert tone="error">{rejected}</FormAlert>}
            <BookingGrantField label={t("task.bookingGrant.users")} required>
              {(ids) => (
                <EntityMultiAutocomplete
                  id={ids.controlId}
                  url="user/autosearch?search=:search"
                  value={users}
                  onChange={setUsers}
                  removeLabel={(entry) =>
                    `${t("delete")}: ${entry.displayName}`
                  }
                  sorted
                />
              )}
            </BookingGrantField>
            <BookingGrantField
              label={t("task.bookingGrant.positions._")}
              required
            >
              {() => (
                <BookingGrantPositions
                  tasks={tasks}
                  truncated={info.tasksTruncated === true}
                  value={positions}
                  onChange={setPositions}
                />
              )}
            </BookingGrantField>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <BookingGrantField
                label={t("task.bookingGrant.validFrom")}
                required
              >
                {(ids) => (
                  <DateInput
                    id={ids.controlId}
                    value={validFrom}
                    onChange={setValidFrom}
                  />
                )}
              </BookingGrantField>
              <BookingGrantField
                label={t("task.bookingGrant.validUntil")}
                required
              >
                {(ids) => (
                  <DateInput
                    id={ids.controlId}
                    value={validUntil}
                    onChange={setValidUntil}
                  />
                )}
              </BookingGrantField>
            </div>
            <BookingGrantField label={t("comment")}>
              {(ids) => (
                <Textarea
                  id={ids.controlId}
                  value={comment}
                  rows={2}
                  onChange={(event) => setComment(event.target.value)}
                />
              )}
            </BookingGrantField>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              {t("cancel")}
            </Button>
            <Button
              type="button"
              disabled={grant.isPending}
              onClick={() => grant.mutate()}
            >
              {grant.isPending && <Spinner className="h-4 w-4 border-2" />}
              {t(actionKey)}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
