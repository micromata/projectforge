"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  CHAT_ROOMS_QUERY_KEY,
  deleteChatRoom,
  fetchChatRoom,
  fetchChatRoomDefaults,
  saveChatRoom,
} from "@/lib/rs/chat-rooms";
import type { ChatRoom } from "@/lib/rs/chat-types";
import { toast } from "@/lib/toast";
import { ChatRoomForm } from "./chat-room-form";

const NEW_ROOM: ChatRoom = {
  id: null,
  name: "",
  description: null,
  moderators: [],
  users: [],
  groups: [],
};

/**
 * Creating ([roomId] null) or editing a chat room (PF-CHAT-ROOM): name, description, participating users and
 * groups, moderators, how long its files are kept. Saved, [onSaved] gets the room's conversation. Owner and moderators may delete it.
 */
export function ChatRoomDialog({
  open,
  roomId,
  onClose,
  onSaved,
  onDeleted = onClose,
}: {
  open: boolean;
  roomId: number | null;
  onClose: () => void;
  onSaved: (conversationId: number | null) => void;
  /** After deleting the room; by default [onClose]. */
  onDeleted?: () => void;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["chatRoom", roomId],
    queryFn: ({ signal }) => fetchChatRoom(roomId!, signal),
    enabled: open && roomId != null,
  });
  const defaults = useQuery({
    queryKey: ["chatRoomDefaults"],
    queryFn: ({ signal }) => fetchChatRoomDefaults(signal),
    enabled: open && roomId == null,
  });
  const [error, setError] = useState<string | null>(null);
  const done = () => {
    setError(null);
    void queryClient.invalidateQueries({ queryKey: CHAT_ROOMS_QUERY_KEY });
  };
  const save = useMutation({
    mutationFn: saveChatRoom,
    onSuccess: (result) => {
      done();
      void queryClient.invalidateQueries({ queryKey: ["chatRoom", result.id] });
      onSaved(result.conversationId);
    },
    onError: (e: Error) => setError(e.message),
  });
  const remove = useMutation({
    mutationFn: deleteChatRoom,
    onSuccess: () => {
      done();
      onDeleted();
    },
    onError: (e: Error) => toast.error(e.message),
  });
  const room =
    roomId == null
      ? defaults.data && { ...NEW_ROOM, ...defaults.data }
      : query.data;
  const title = roomId == null ? t("chat.room.new") : t("chat.room.edit");

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription className="sr-only">{title}</DialogDescription>
        </DialogHeader>
        {error && <FormAlert tone="error">{error}</FormAlert>}
        {(query.isError || defaults.isError) && (
          <FormAlert tone="error">{t("chat.conversation.notFound")}</FormAlert>
        )}
        {!room && !query.isError && !defaults.isError && (
          <Spinner className="h-5 w-5 border-2" />
        )}
        {room && (
          <ChatRoomForm
            key={room.id ?? "new"}
            room={room}
            saving={save.isPending}
            onCancel={onClose}
            onSave={(changed) => save.mutate(changed)}
            onDelete={
              room.id != null && room.canEdit
                ? () => remove.mutate(room.id!)
                : undefined
            }
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
