import { z } from "zod";
import { KOST_ZUWEISUNG_METADATA } from "@/lib/metadata/kost-zuweisung.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * One cost assignment of an invoice position — `KostZuweisung`, the third nesting level of either
 * invoice form. Identical on the outgoing and the incoming invoice (both nest `KostZuweisungDO` under
 * their positions), so schema, type and the two value helpers live here and both features reuse them.
 *
 * Every rule below comes from `KostZuweisungDO` through `lib/metadata/kost-zuweisung.generated.ts`, the
 * same source the field components read.
 */
const k = fromMetadata(KOST_ZUWEISUNG_METADATA);

/**
 * A referenced entity the metadata has no field for: `kost1`/`kost2` are `Kost1DO`/`Kost2DO`, for which
 * there is no `UIDataType`, so `ElementsRegistry` never reports them and the generator cannot carry
 * them. Written by id like every other reference.
 *
 * A type alias rather than an interface, so it satisfies the index signature of the `looseObject`.
 */
export type EntityRefDto = {
  id: number;
  displayName?: string;
};

const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/**
 * One cost assignment as the form holds it.
 *
 * `index` travels back untouched, and a removed row stays with `deleted = true`:
 * `RechnungsPositionDO.kostZuweisungen` carries `autoUpdateCollectionEntries` but no
 * `@SoftDeleteCollection`, and `KostZuweisungDO.equals` matches on `(index, owner)` — so an omitted or
 * renumbered row reads as "removed" to the collection handler and is deleted physically, history and all.
 */
export const kostZuweisungSchema = z.object({
  id: z.number().nullable(),
  deleted: z.boolean(),
  /** 0-based, unlike a position's 1-based number (`KostZuweisungDO.addKostZuweisung`). */
  index: z.number().nullable(),
  netto: k.decimalField("netto"),
  kost1: entityRef,
  kost2: entityRef,
  comment: k.nullableString("comment"),
});

export type KostZuweisungValues = z.infer<typeof kostZuweisungSchema>;

/** One cost assignment as the DTO travels — `KostZuweisung`, the third nesting level. */
export interface KostZuweisungDto {
  id?: number | null;
  deleted?: boolean;
  /** Position within its own position's list, **0-based** (`KostZuweisungDO.addKostZuweisung`). */
  index?: number | null;
  netto?: number | null;
  kost1?: EntityRefDto | null;
  kost2?: EntityRefDto | null;
  comment?: string | null;
}

/** Normalises a DTO cost assignment into form values — every absent field becomes `null`. */
export function toKostZuweisungValues(
  assignment: KostZuweisungDto
): KostZuweisungValues {
  return {
    id: assignment.id ?? null,
    deleted: assignment.deleted === true,
    index: assignment.index ?? null,
    netto: assignment.netto ?? null,
    kost1: assignment.kost1 ?? null,
    kost2: assignment.kost2 ?? null,
    comment: assignment.comment ?? null,
  };
}

/**
 * A fresh cost assignment.
 *
 * @param index What [nextKostZuweisungIndex] yields for the rows the position currently holds.
 * @param predecessor The row it is added below, if any. Its two cost units are proposed — splitting a
 *   position across cost 2 units usually keeps cost 1, and Wicket's dialog carries them over the same
 *   way.
 * @param netto What of the position is still unassigned, which is what the new row is most likely for:
 *   the whole net sum on the first row, the rest on a later one. A proposal like the two cost units and
 *   nothing more — the field stays editable, and the Fehlbetrag still says whether it adds up.
 * @param defaultKost2 The cost unit used where there is no predecessor to take one from — the outgoing
 *   invoice preselects its project's first active cost unit here; the incoming invoice has no project
 *   and passes none.
 */
export function emptyKostZuweisungValues(
  index: number,
  predecessor?: KostZuweisungValues,
  netto?: number | null,
  defaultKost2?: KostZuweisungValues["kost2"]
): KostZuweisungValues {
  return toKostZuweisungValues({
    index,
    netto,
    kost1: predecessor?.kost1 ?? null,
    kost2: predecessor?.kost2 ?? defaultKost2 ?? null,
  });
}
