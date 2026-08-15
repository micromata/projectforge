"use client";

import { useCallback, useMemo, useSyncExternalStore } from "react";
import { useAuth } from "@/hooks/use-auth";

/** How many entries the quick access palette offers before the user has typed anything. */
const MAX_RECENT = 5;

/**
 * Per user, not per browser: a shared workstation would otherwise hand the next login the history
 * of the previous one, and the entries a user may see are their own.
 */
function storageKey(username: string): string {
  return `pf.next.recentMenuEntries.${username}`;
}

/**
 * The menu entries the user picked last, most recent first — the quick access palette's head start.
 *
 * Only the keys are kept, never the titles or urls: the menu tree is the truth about both, and it is
 * access-filtered per user, so an entry the user may no longer see simply stops resolving instead of
 * lingering here as a dead row.
 *
 * Kept in localStorage rather than a UserPref: it is a convenience of this browser, and a REST round
 * trip per opened palette would be paid for by everyone to help the few who switch machines.
 */
export function useRecentMenuEntries(): {
  recentKeys: string[];
  remember: (key: string) => void;
} {
  const { user } = useAuth();
  const username = user?.username;
  const store = useMemo(
    () => (username ? recentStore(storageKey(username)) : null),
    [username]
  );

  // localStorage is an external store, and this is how React reads one: the server snapshot is
  // empty, because the app is a static export and what is prerendered has no browser to ask.
  const recentKeys = useSyncExternalStore(
    store?.subscribe ?? subscribeToNothing,
    store?.getSnapshot ?? getNothing,
    getNothing
  );

  const remember = useCallback((key: string) => store?.remember(key), [store]);

  return { recentKeys, remember };
}

const EMPTY: string[] = [];
const getNothing = () => EMPTY;
const subscribeToNothing = () => () => {};

/** One store per storage key, so a re-render keeps the snapshot it was given. */
const stores = new Map<string, ReturnType<typeof createRecentStore>>();

function recentStore(key: string) {
  const existing = stores.get(key);
  if (existing) return existing;
  const created = createRecentStore(key);
  stores.set(key, created);
  return created;
}

/**
 * A subscribable view of one localStorage entry.
 *
 * The snapshot has to be cached: `useSyncExternalStore` compares it by identity, and parsing the
 * json afresh on every call would answer with a new array each time and re-render forever.
 */
function createRecentStore(key: string) {
  let snapshot: string[] | null = null;
  const listeners = new Set<() => void>();

  const publish = (keys: string[]) => {
    snapshot = keys;
    listeners.forEach((listener) => listener());
  };

  return {
    subscribe(listener: () => void) {
      listeners.add(listener);
      // Another tab of the same session writing its own history: `storage` only fires in the other
      // tabs, so the write below has to notify this one itself.
      const onStorage = (event: StorageEvent) => {
        if (event.key === key) publish(read(key));
      };
      window.addEventListener("storage", onStorage);
      return () => {
        listeners.delete(listener);
        window.removeEventListener("storage", onStorage);
      };
    },

    getSnapshot(): string[] {
      snapshot ??= read(key);
      return snapshot;
    },

    remember(pickedKey: string) {
      const previous = snapshot ?? read(key);
      const next = [
        pickedKey,
        ...previous.filter((k) => k !== pickedKey),
      ].slice(0, MAX_RECENT);
      write(key, next);
      publish(next);
    },
  };
}

/** Tolerates anything that is not the list it wrote: a stale format may not break the palette. */
function read(key: string): string[] {
  try {
    const stored = window.localStorage.getItem(key);
    if (!stored) return EMPTY;
    const parsed: unknown = JSON.parse(stored);
    return Array.isArray(parsed)
      ? parsed.filter((entry): entry is string => typeof entry === "string")
      : EMPTY;
  } catch {
    return EMPTY;
  }
}

/** A full storage (or one a browser denies in private mode) costs the history, nothing more. */
function write(key: string, keys: string[]): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(keys));
  } catch {
    // Ignored on purpose.
  }
}
