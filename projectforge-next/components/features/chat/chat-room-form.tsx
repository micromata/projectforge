"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { Button } from "@/components/ui/button";
import { DialogFooter } from "@/components/ui/dialog";
import { FieldDescription } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { ChatRef, ChatRoom } from "@/lib/rs/chat-types";

/**
 * The fields of a chat room. Participants are chosen from the users and groups the user may see (ROOM-05); the
 * owner takes part anyway and is set by the server. Hints explain what moderators do and that the owner always
 * takes part and moderates (it isn't preset as participant: it couldn't be removed anyway).
 */
export function ChatRoomForm({
  room,
  saving,
  onCancel,
  onSave,
  onDelete,
}: {
  room: ChatRoom;
  saving: boolean;
  onCancel: () => void;
  onSave: (room: ChatRoom) => void;
  onDelete?: () => void;
}) {
  const t = useTranslations();
  const [value, setValue] = useState(room);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const set = (change: Partial<ChatRoom>) =>
    setValue((current) => ({ ...current, ...change }));
  const removeLabel = (entry: ChatRef) =>
    `${t("delete")}: ${entry.displayName}`;
  const readOnly = room.id != null && !room.canEdit;

  return (
    <div className="flex flex-col gap-3">
      <fieldset disabled={readOnly || saving} className="flex flex-col gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatRoomName">{t("chat.room.name")}</Label>
          <Input
            id="chatRoomName"
            value={value.name}
            maxLength={255}
            required
            autoFocus
            onChange={(event) => set({ name: event.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatRoomDescription">
            {t("chat.room.description")}
          </Label>
          <Textarea
            id="chatRoomDescription"
            value={value.description ?? ""}
            maxLength={4000}
            onChange={(event) => set({ description: event.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatRoomUsers">
            {t("chat.room.participantUsers")}
          </Label>
          <EntityMultiAutocomplete
            id="chatRoomUsers"
            url="user/autosearch?search=:search"
            value={value.users}
            onChange={(users) => set({ users })}
            removeLabel={removeLabel}
            sorted
            aria-label={t("chat.room.participantUsers")}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatRoomGroups">
            {t("chat.room.participantGroups")}
          </Label>
          <EntityMultiAutocomplete
            id="chatRoomGroups"
            url="group/autosearch?search=:search"
            value={value.groups}
            onChange={(groups) => set({ groups })}
            removeLabel={removeLabel}
            sorted
            aria-label={t("chat.room.participantGroups")}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatRoomModerators">
            {t("chat.room.moderators._")}
          </Label>
          <EntityMultiAutocomplete
            id="chatRoomModerators"
            url="user/autosearch?search=:search"
            value={value.moderators}
            onChange={(moderators) => set({ moderators })}
            removeLabel={removeLabel}
            sorted
            aria-label={t("chat.room.moderators._")}
          />
          <FieldDescription>{t("chat.room.moderators.info")}</FieldDescription>
        </div>
      </fieldset>
      <FieldDescription>
        {room.owner && (
          <>
            {t("chat.room.owner._")}: {room.owner.displayName}.{" "}
          </>
        )}
        {t("chat.room.owner.info")}
      </FieldDescription>
      <DialogFooter className="items-center">
        {onDelete && (
          <Button
            type="button"
            variant="destructive"
            className="sm:mr-auto"
            onClick={() => setConfirmDelete(true)}
          >
            {t("chat.room.delete._")}
          </Button>
        )}
        <Button type="button" variant="outline" onClick={onCancel}>
          {t("chat.message.cancel")}
        </Button>
        {!readOnly && (
          <Button
            type="button"
            disabled={!value.name.trim() || saving}
            onClick={() => onSave(value)}
          >
            {t("chat.message.save")}
          </Button>
        )}
      </DialogFooter>
      {onDelete && (
        <ConfirmDialog
          open={confirmDelete}
          onOpenChange={setConfirmDelete}
          title={t("chat.room.delete._")}
          description={t("chat.room.delete.confirm", { arg0: room.name })}
          confirmLabel={t("chat.room.delete._")}
          destructive
          onConfirm={onDelete}
        />
      )}
    </div>
  );
}
