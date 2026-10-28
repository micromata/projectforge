"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
  LockPasswordIcon,
  Message01Icon,
} from "@hugeicons/core-free-icons";
import { matchesSearchTerm } from "@/components/data-table/filter-groups";
import { FormAlert } from "@/components/shared/form-alert";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { SearchInput } from "@/components/shared/list/search-input";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { richTextToPlainText } from "@/components/shared/rich-text";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { useFeatureEnabled } from "@/hooks/use-feature-enabled";
import {
  CHAT_HOST_DATA_TRANSFER,
  chatHostListQueryKey,
  fetchChatHostConversations,
} from "@/lib/rs/chat";
import { CHAT_DIRECT_QUERY_KEY, fetchChatDirect } from "@/lib/rs/chat-direct";
import { CHAT_ROOMS_QUERY_KEY, fetchChatRooms } from "@/lib/rs/chat-rooms";
import { ChatDirectCandidates } from "./chat-direct-candidates";
import { ChatDirectDialog } from "./chat-direct-dialog";
import { ChatDirectList } from "./chat-direct-list";
import { ChatHostList } from "./chat-host-list";
import { ChatSecretDialog } from "./chat-secret-dialog";
import { ChatRoomDialog } from "./chat-room-dialog";
import { ChatRoomList } from "./chat-room-list";
import { ChatMessageSearch } from "./chat-search-hit-list";
import { useDirectCandidates } from "./use-direct-candidates";
import { ChatAlertSettingsPopover } from "./chat-alert-settings-popover";

/**
 * The chat of the user (`/next/chat`): the direct messages (PF-CHAT-DM), the chat rooms (PF-CHAT-ROOM, ROOM-06) and
 * the chats of the data transfer areas with messages (PF-CHAT-DT, with the plugin only), the ones with the newest
 * messages first, with unread counts; starting a direct message, creating a room and
 * editing those the user owns or moderates. Each opens its conversation. A search box narrows the lists, on the
 * entries already loaded (other one, room name, description, area name); from three characters on, it also searches
 * the messages of all readable conversations (SRCH-02), listed below with the newest match each. Minimal on purpose: the chat area with the
 * conversations beside the open one follows (ROOM-08).
 */
export function ChatRoomsPage() {
  const t = useTranslations();
  const router = useRouter();
  const query = useQuery({
    queryKey: CHAT_ROOMS_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatRooms(signal),
  });
  const direct = useQuery({
    queryKey: CHAT_DIRECT_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatDirect(signal),
  });
  const dataTransferEnabled = useFeatureEnabled("datatransfer");
  const dataTransfer = useQuery({
    queryKey: chatHostListQueryKey(CHAT_HOST_DATA_TRANSFER),
    queryFn: ({ signal }) =>
      fetchChatHostConversations(CHAT_HOST_DATA_TRANSFER, signal),
    enabled: dataTransferEnabled,
  });
  const [dialog, setDialog] = useState<{ roomId: number | null } | null>(null);
  const [directDialog, setDirectDialog] = useState(false);
  const [secretDialog, setSecretDialog] = useState(false);
  const [search, setSearch] = useState("");
  const directFound = useMemo(
    () =>
      direct.data?.filter((entry) =>
        matchesSearchTerm(search, entry.partner.displayName)
      ),
    [direct.data, search]
  );
  const candidates = useDirectCandidates(search, direct.data);
  const roomsFound = useMemo(
    () =>
      query.data?.filter((room) =>
        matchesSearchTerm(
          search,
          room.name,
          room.description ? richTextToPlainText(room.description) : undefined
        )
      ),
    [query.data, search]
  );
  const dataTransferFound = useMemo(
    () =>
      dataTransfer.data?.filter((entry) =>
        matchesSearchTerm(search, entry.title)
      ),
    [dataTransfer.data, search]
  );
  // An empty list because of the search, not because there is nothing yet.
  const nothingFound = (
    <p className="text-sm text-muted-foreground">{t("nothingFound")}</p>
  );

  return (
    <PageShell>
      <PageTitleRow category={t("menu.common")} title={t("chat.title")}>
        <Button variant="outline" onClick={() => setDirectDialog(true)}>
          <HugeiconsIcon icon={Message01Icon} data-icon="inline-start" />
          {t("chat.direct.new")}
        </Button>
        <HintTooltip text={t("chat.secret.newTooltip")}>
          <Button variant="outline" onClick={() => setSecretDialog(true)}>
            <HugeiconsIcon
              icon={LockPasswordIcon}
              strokeWidth={2}
              data-icon="inline-start"
            />
            {t("chat.secret.new")}
          </Button>
        </HintTooltip>
        <Button onClick={() => setDialog({ roomId: null })}>
          <HugeiconsIcon icon={Add01Icon} data-icon="inline-start" />
          {t("chat.room.new")}
        </Button>
        {/* Where the user finds how a confidential conversation offered draws attention, without being offered one. */}
        <ChatAlertSettingsPopover />
      </PageTitleRow>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 px-4 pt-3">
        <div className="relative w-64 max-w-full">
          <SearchInput value={search} onChange={setSearch} focusIfIdle />
        </div>
      </div>
      <section
        className="flex flex-col gap-3 px-4 pt-2 pb-4"
        aria-labelledby="chatDirectHeading"
      >
        <h2 id="chatDirectHeading" className="text-sm font-semibold">
          {t("chat.direct._")}
        </h2>
        {direct.isError && (
          <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
        )}
        {direct.isPending && <Spinner className="h-5 w-5 border-2" />}
        {direct.data?.length === 0 && (
          <p className="text-sm text-muted-foreground">
            {t("chat.direct.empty")}
          </p>
        )}
        {direct.data &&
          direct.data.length > 0 &&
          directFound?.length === 0 &&
          candidates.length === 0 &&
          nothingFound}
        {directFound && directFound.length > 0 && (
          <ChatDirectList entries={directFound} highlight={search} />
        )}
        {candidates.length > 0 && (
          <ChatDirectCandidates users={candidates} highlight={search} />
        )}
      </section>
      <section
        className="flex flex-col gap-3 px-4 pb-6"
        aria-labelledby="chatRoomsHeading"
      >
        <h2 id="chatRoomsHeading" className="text-sm font-semibold">
          {t("chat.rooms._")}
        </h2>
        {query.isError && (
          <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
        )}
        {query.isPending && <Spinner className="h-5 w-5 border-2" />}
        {query.data?.length === 0 && (
          <p className="text-sm text-muted-foreground">
            {t("chat.rooms.empty")}
          </p>
        )}
        {query.data &&
          query.data.length > 0 &&
          roomsFound?.length === 0 &&
          nothingFound}
        {roomsFound && roomsFound.length > 0 && (
          <ChatRoomList
            rooms={roomsFound}
            highlight={search}
            onEdit={(roomId) => setDialog({ roomId })}
          />
        )}
      </section>
      {dataTransferEnabled && (
        <section
          className="flex flex-col gap-3 px-4 pb-6"
          aria-labelledby="chatDataTransferHeading"
        >
          <h2 id="chatDataTransferHeading" className="text-sm font-semibold">
            {t("chat.dataTransfer._")}
          </h2>
          {dataTransfer.isError && (
            <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
          )}
          {dataTransfer.isPending && <Spinner className="h-5 w-5 border-2" />}
          {dataTransfer.data?.length === 0 && (
            <p className="text-sm text-muted-foreground">
              {t("chat.dataTransfer.empty")}
            </p>
          )}
          {dataTransfer.data &&
            dataTransfer.data.length > 0 &&
            dataTransferFound?.length === 0 &&
            nothingFound}
          {dataTransferFound && dataTransferFound.length > 0 && (
            <ChatHostList entries={dataTransferFound} highlight={search} />
          )}
        </section>
      )}
      <ChatMessageSearch term={search} />
      <ChatDirectDialog
        open={directDialog}
        onClose={() => setDirectDialog(false)}
      />
      <ChatSecretDialog
        open={secretDialog}
        onClose={() => setSecretDialog(false)}
      />
      <ChatRoomDialog
        open={dialog != null}
        roomId={dialog?.roomId ?? null}
        onClose={() => setDialog(null)}
        onSaved={(conversationId) => {
          setDialog(null);
          if (conversationId) router.push(`/chat/${conversationId}`);
        }}
      />
    </PageShell>
  );
}
