import type { InactiveFlags } from "@/lib/page-def/reference";

/** What is carried from one multi picker to another — the shape of [EntityRef], kept here to stay pure. */
export interface TransferredRef extends InactiveFlags {
  id: number;
  displayName: string;
}

const MIME_PREFIX = "application/x-projectforge-refs+";

/**
 * The kind of entity a picker holds, read from its lookup url: `user/autosearch?…` → `user`. A picker
 * whose url names something else (`vacation/users`) declares its kind instead.
 */
export function refTypeOfUrl(url: string): string {
  return url.split(/[/?]/, 1)[0];
}

/**
 * The kind sits in the MIME type rather than in the payload: while dragging, a drop target may read
 * only the types of what is dragged, not the data — and it has to tell already then whether it takes it.
 * Lower case, because `DataTransfer.types` reports them so.
 */
function mimeOf(type: string): string {
  return `${MIME_PREFIX}${type.toLowerCase()}`;
}

/**
 * Puts references onto a drag or the clipboard: for another picker of the same kind as they are, and as
 * their names, one per line, for anything else (a mail, a spreadsheet). Placeholders (a negative id, see
 * kost-numbers-picker) are left out: nothing can take them.
 */
export function writeRefs(
  data: DataTransfer,
  type: string,
  refs: readonly TransferredRef[]
): void {
  const real = refs.filter((ref) => ref.id > 0);
  data.setData(
    mimeOf(type),
    JSON.stringify(
      real.map(({ id, displayName, deleted, deactivated }) => ({
        id,
        displayName,
        deleted,
        deactivated,
      }))
    )
  );
  data.setData("text/plain", real.map((ref) => ref.displayName).join("\n"));
}

/** Whether `data` carries references of this kind — answerable while dragging, see [mimeOf]. */
export function carriesRefs(data: DataTransfer, type: string): boolean {
  return Array.from(data.types).includes(mimeOf(type));
}

/**
 * The references of this kind `data` carries; empty when it carries none, or something that isn't a
 * list of references (another page, another version, someone's hand-made clipboard).
 */
export function readRefs(data: DataTransfer, type: string): TransferredRef[] {
  const raw = data.getData(mimeOf(type));
  if (!raw) return [];
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return [];
  }
  if (!Array.isArray(parsed)) return [];
  return parsed.flatMap((entry): TransferredRef[] => {
    if (typeof entry !== "object" || entry === null) return [];
    const { id, displayName, deleted, deactivated } = entry as Record<
      string,
      unknown
    >;
    if (!Number.isSafeInteger(id) || (id as number) <= 0) return [];
    if (typeof displayName !== "string") return [];
    return [
      {
        id: id as number,
        displayName,
        ...(deleted === true && { deleted }),
        ...(deactivated === true && { deactivated }),
      },
    ];
  });
}
