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
import { DateInput } from "@/components/shared/date-input";
import { FormAlert } from "@/components/shared/form-alert";
import { PersonSelect } from "@/components/shared/person-select";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { plusDays } from "@/lib/date-period-math";
import type { TaskKost2 } from "@/lib/rs/task";
import {
  grantTaskBooking,
  type TaskBookingGrantResult,
} from "@/lib/rs/task-booking-grant";
import { todayOf } from "@/lib/user-zone";
import {
  BookingGrantField,
  BookingGrantInvitationCheckbox,
} from "./booking-grant-field";
import { BookingGrantKost2Select } from "./booking-grant-kost2-select";

/**
 * Grants a user the booking of own time sheets on the task: the user, the period, optionally one cost unit
 * and a comment, directly or as an invitation link only this user may accept.
 *
 * Written right away through its own endpoint, not with the task form around it. What is wrong is the
 * backend's to say: its message is shown above the fields.
 */
export function BookingGrantDialog({
  taskId,
  kost2List,
  onDone,
  onClose,
}: {
  taskId: number;
  kost2List: TaskKost2[];
  onDone: (result: TaskBookingGrantResult) => void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const today = todayOf(ctx);
  const [user, setUser] = useState<EntityRef | null>(null);
  const [validFrom, setValidFrom] = useState<string | null>(today);
  const [validUntil, setValidUntil] = useState<string | null>(
    plusDays(today, 13)
  );
  const [kost2Id, setKost2Id] = useState<number | null>(null);
  const [comment, setComment] = useState("");
  const [asInvitation, setAsInvitation] = useState(false);
  const grant = useMutation({
    mutationFn: () =>
      grantTaskBooking({
        taskId,
        userId: user?.id ?? null,
        validFrom,
        validUntil,
        kost2Id,
        comment: comment.trim() || null,
        asInvitation,
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
        <DialogContent onOpenAutoFocus={(event) => event.preventDefault()}>
          <DialogHeader>
            <DialogTitle>{t("task.bookingGrant.add")}</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            {rejected && <FormAlert tone="error">{rejected}</FormAlert>}
            <BookingGrantField label={t("user._")} required>
              {(ids) => (
                <PersonSelect
                  id={ids.controlId}
                  value={user}
                  onChange={setUser}
                  selectMe={false}
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
            {kost2List.length > 0 && (
              <BookingGrantField label={t("fibu.kost2._")}>
                {(ids) => (
                  <BookingGrantKost2Select
                    ids={ids}
                    kost2List={kost2List}
                    value={kost2Id}
                    onChange={setKost2Id}
                  />
                )}
              </BookingGrantField>
            )}
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
            <BookingGrantInvitationCheckbox
              checked={asInvitation}
              onChange={setAsInvitation}
            />
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
              {t("task.bookingGrant.add")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
