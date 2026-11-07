"use client";

import { useEffect, useRef } from "react";
import { useAuth } from "@/hooks/use-auth";
import { isInApp } from "@/lib/app-mode";

type BadgeSource = {
  chatUnread: number;
  notifications: { openCount: number; latestId?: number | null } | null;
};

/** What the app's own badges show (the chat tab, the bell): a change of it is told to the app. */
export function badgeKey({ chatUnread, notifications }: BadgeSource): string {
  return `${chatUnread}/${notifications?.openCount ?? 0}/${notifications?.latestId ?? ""}`;
}

type AppBridge = { postMessage(message: string): void };

/**
 * In the app (projectforge-app), its tab bar shows the unread chat messages and the notifications, loaded by itself
 * once a minute. When these pages change them (a chat read, a notification done), they tell the app at once, so its
 * badges don't lag behind: `{"pf":"status"}`, no data, the app loads its status again. Not on the first load.
 */
export function useAppStatusSignal() {
  const { chatUnread, notifications, isAuthenticated } = useAuth();
  const key = isAuthenticated ? badgeKey({ chatUnread, notifications }) : null;
  const last = useRef<string | null>(null);

  useEffect(() => {
    if (key == null || !isInApp()) return;
    const previous = last.current;
    last.current = key;
    if (previous == null || previous === key) return;
    const bridge = (window as { ReactNativeWebView?: AppBridge })
      .ReactNativeWebView;
    bridge?.postMessage('{"pf":"status"}');
  }, [key]);
}
