// A short log of the signals of this browser (sound), to see afterwards whether it rang and why not. Kept in
// localStorage, the last few entries only: it is a setting of the browser like the sound itself.

import type { AttentionSoundOutcome } from "@/lib/attention-sound";

const STORAGE_KEY = "pf.chat.ringLog";
const KEEP = 50;

export interface RingLogEntry {
  /** Epoch milliseconds. */
  at: number;
  /** True if the sound was played. */
  rang: boolean;
  /** Why it was signalled: a confidential conversation offered. */
  reason: "secretOffer";
  /** Why it stayed silent, if it did. */
  silent?: "muted" | "blocked";
  /** Who, e.g. the initiator of the offer. */
  arg?: string;
}

export function readRingLog(): RingLogEntry[] {
  try {
    const parsed: unknown = JSON.parse(
      window.localStorage.getItem(STORAGE_KEY) ?? "[]"
    );
    return Array.isArray(parsed) ? (parsed as RingLogEntry[]) : [];
  } catch {
    return [];
  }
}

export function addRingLogEntry(entry: {
  outcome: AttentionSoundOutcome;
  reason: RingLogEntry["reason"];
  arg?: string;
}): void {
  const rang = entry.outcome === "played";
  const logged: RingLogEntry = {
    at: Date.now(),
    rang,
    reason: entry.reason,
    arg: entry.arg,
    silent: rang ? undefined : (entry.outcome as "muted" | "blocked"),
  };
  try {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([...readRingLog(), logged].slice(-KEEP))
    );
    window.dispatchEvent(new Event(RING_LOG_EVENT));
  } catch {
    // Private mode without storage: no log.
  }
}

export function clearRingLog(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY);
    window.dispatchEvent(new Event(RING_LOG_EVENT));
  } catch {
    // Nothing to clear.
  }
}

/** Fired on this window after the log changed, so an open view reloads it. */
export const RING_LOG_EVENT = "pf-ring-log";
