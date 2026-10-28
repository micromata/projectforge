"use client";

import { useEffect } from "react";

const INTERVAL_MS = 1_500;

/**
 * Alternates the window title between [alert] and the page's own title while the tab is hidden, so the tab bar
 * shows that something waits; null stops it. The page's title (useDocumentTitle) is taken as it is at each
 * change and restored when the alert ends or the tab is shown.
 */
export function useBlinkingTitle(alert: string | null) {
  useEffect(() => {
    if (!alert) return;
    let original: string | null = null;
    let timer: ReturnType<typeof setInterval> | undefined;
    const restore = () => {
      clearInterval(timer);
      timer = undefined;
      if (original != null && document.title === alert) {
        document.title = original;
      }
    };
    const start = () => {
      if (timer || !document.hidden) return;
      timer = setInterval(() => {
        if (document.title === alert) {
          if (original != null) document.title = original;
        } else {
          original = document.title;
          document.title = alert;
        }
      }, INTERVAL_MS);
    };
    const onVisibility = () => (document.hidden ? start() : restore());
    start();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      restore();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [alert]);
}
