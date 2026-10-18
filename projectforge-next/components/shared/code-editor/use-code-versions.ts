"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { CodeVersion, CodeVersionSource } from "./types";

/** The code being edited, not yet saved. */
export const EDITED = "@edited";
/** The code as stored. */
export const STORED = "@stored";
const BACKUP_PREFIX = "backup:";

/** One version the diff can show on a side: the edited code, the stored one, or a former version. */
export interface CodeVersionOption {
  value: string;
  /** Set for a former version. */
  backup?: CodeVersion;
}

/**
 * The two versions the diff compares and their code. Without a choice of the user's: the stored code
 * against the edited one, or — where nothing was edited — the newest former version against the stored
 * code. Starts afresh with every opening of the dialog (its body is mounted anew).
 */
export function useCodeVersions({
  original,
  current,
  source,
}: {
  original: string;
  current: string;
  source?: CodeVersionSource;
}) {
  const changed = original !== current;
  const list = useQuery({
    queryKey: [...(source?.queryKey ?? []), "list"],
    queryFn: ({ signal }) => source!.list(signal),
    enabled: !!source,
  });
  const backups = list.data ?? [];
  const options: CodeVersionOption[] = [
    ...(changed ? [{ value: EDITED }] : []),
    { value: STORED },
    ...backups.map((backup) => ({ value: BACKUP_PREFIX + backup.key, backup })),
  ];
  const [leftChoice, setLeft] = useState<string | null>(null);
  const [rightChoice, setRight] = useState<string | null>(null);
  const newest = backups[0] ? BACKUP_PREFIX + backups[0].key : STORED;
  const left = leftChoice ?? (changed ? STORED : newest);
  const right = rightChoice ?? (changed ? EDITED : STORED);
  const leftCode = useVersionCode(left, original, current, source);
  const rightCode = useVersionCode(right, original, current, source);
  return {
    options,
    left,
    right,
    setLeft,
    setRight,
    leftCode,
    rightCode,
    /** The former versions are still being listed. */
    listing: list.isPending && !!source,
    /** There are none (once listed). */
    noBackups: !!source && list.isSuccess && backups.length === 0,
  };
}

/** The code of a version; undefined while a former version's code is still being fetched. */
function useVersionCode(
  value: string,
  original: string,
  current: string,
  source?: CodeVersionSource
): string | undefined {
  const key = value.startsWith(BACKUP_PREFIX)
    ? value.slice(BACKUP_PREFIX.length)
    : null;
  const backup = useQuery({
    queryKey: [...(source?.queryKey ?? []), "version", key],
    queryFn: ({ signal }) => source!.load(key!, signal),
    enabled: !!source && key != null,
    // A former version never changes.
    staleTime: Infinity,
  });
  if (value === EDITED) return current;
  if (value === STORED) return original;
  return backup.data;
}
