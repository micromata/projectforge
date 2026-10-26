"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon, Message01Icon } from "@hugeicons/core-free-icons";
import { CommandGroup, CommandItem } from "@/components/ui/command";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useOpenDirect } from "@/hooks/use-open-direct";
import { useAuth } from "@/hooks/use-auth";
import { useDataSearch } from "@/hooks/use-data-search";
import { useFeatureEnabled } from "@/hooks/use-feature-enabled";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { areaMoreUrl, type SearchHit } from "@/lib/rs/search";

/** The area of the users in the data search (`DaoConst.USER`). */
const USER_AREA = "user";

/**
 * The live data hits in the menu search: a few matches per area, addresses first, below the menu hits. With the
 * chat on, the users lead instead, above the menu ([part] `users`, the others `others`): somebody looked for by name
 * is mostly to be written to.
 *
 * Renders as `CommandGroup`s so it joins the same cmdk list — with `shouldFilter={false}` on the root the
 * arrow keys walk the items in DOM order, so the menu leads and the data follows. Each hit navigates to its
 * record via [useNavigateMenuUrl], collapsing the search slot on the way; an area with more matches ends in a
 * row into the dedicated search page, scoped to that area. A user other than oneself offers to start a chat
 * (direct message, where files can be sent as well), by its button or Ctrl/Cmd+Enter (see MainMenuDropdown). Who may
 * not open the user pages gets the users as contacts (no `viewUrl`, no "more" row): the row starts the chat as well.
 */
export function QuickDataHits({
  term,
  part,
  onNavigate,
}: {
  term: string;
  /** The users only (leading, with the chat on) or the other areas; both from the same (cached) search. */
  part: "users" | "others";
  /** Collapses the search slot once a destination is chosen. */
  onNavigate: () => void;
}) {
  const t = useTranslations("search");
  const navigate = useNavigateMenuUrl();
  const { user } = useAuth();
  const chat = useFeatureEnabled("CHAT");
  const { data } = useDataSearch(term, 3, chat);
  const openDirect = useOpenDirect();

  const areas = (data?.areas ?? []).filter(
    (area) => (part === "users") === (chat && area.areaId === USER_AREA)
  );
  if (areas.length === 0) return null;

  return (
    <>
      {areas.map((area) => {
        const moreUrl =
          area.hasMore && area.listUrl
            ? areaMoreUrl({ listUrl: area.listUrl }, term)
            : null;
        return (
          <CommandGroup key={area.areaId} heading={area.title}>
            {area.hits.map((hit) => (
              <CommandItem
                // Globally unique: a data hit and a menu entry could otherwise share a value and the arrow keys
                // would select them together (see the menu items' `group:key` for the same reason).
                key={`${area.areaId}:${hit.id}`}
                value={`data:${area.areaId}:${hit.id}`}
                onSelect={() =>
                  hit.viewUrl
                    ? navigate(hit.viewUrl, onNavigate)
                    : openDirect.mutate(hit.id, { onSuccess: onNavigate })
                }
              >
                {/* Grows, so what follows sits at the right (the item ends in a hidden tick taking `ml-auto` too). */}
                <span className="min-w-0 flex-1 truncate">
                  <HighlightedText text={hit.displayName} query={term} />
                </span>
                {hit.secondaryInfo ? (
                  <span className="ml-auto truncate text-xs text-muted-foreground">
                    {hit.secondaryInfo}
                  </span>
                ) : null}
                {chat &&
                  area.areaId === USER_AREA &&
                  user &&
                  hit.id !== user.userId && (
                    <StartChatButton
                      hit={hit}
                      openDirect={openDirect}
                      onNavigate={onNavigate}
                    />
                  )}
              </CommandItem>
            ))}
            {moreUrl && (
              <CommandItem
                value={`data-more:${area.areaId}`}
                onSelect={() => navigate(moreUrl, onNavigate)}
              >
                <HugeiconsIcon icon={ArrowRight01Icon} />
                <span className="truncate">
                  {t("next.moreInArea", { arg0: area.title })}
                </span>
              </CommandItem>
            )}
          </CommandGroup>
        );
      })}
    </>
  );
}

/**
 * Opens the direct message with the user of the hit, the search collapsing once it is there. Marked as the action
 * of its row (`data-quick-action`) for Ctrl/Cmd+Enter; it doesn't select the row (which goes to the user).
 */
function StartChatButton({
  hit,
  openDirect,
  onNavigate,
}: {
  hit: SearchHit;
  /** Shared by the rows, which start the chat as well for a contact. */
  openDirect: ReturnType<typeof useOpenDirect>;
  onNavigate: () => void;
}) {
  const t = useTranslations();
  const files = useFeatureEnabled("datatransfer");
  const label = t(files ? "chat.direct.startOrSendFile" : "chat.direct.start");
  return (
    <HintTooltip text={label}>
      <button
        type="button"
        data-quick-action
        aria-label={`${hit.displayName}: ${label}`}
        disabled={openDirect.isPending}
        onPointerDown={(event) => event.stopPropagation()}
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
          openDirect.mutate(hit.id, { onSuccess: onNavigate });
        }}
        className="ml-auto flex shrink-0 cursor-pointer items-center gap-1 rounded-sm bg-brand-yellow-dark px-1.5 py-0.5 text-xs font-medium text-black hover:bg-brand-yellow-dark/85 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
      >
        <HugeiconsIcon icon={Message01Icon} />
        <span className="hidden sm:inline">{label}</span>
      </button>
    </HintTooltip>
  );
}
