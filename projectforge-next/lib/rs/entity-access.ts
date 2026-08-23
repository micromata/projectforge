/**
 * What the logged-in user may do with the entity being edited, as its own DTO reports it.
 *
 * Not `UILayout.UserAccess`: that one travels with the *layout*, and `GET /rs/{entity}/{id}` of a hand
 * built page passes none. So the flags are fields of the entity — of every entity whose DTO implements
 * `EntityAccessSupport`, filled once in `AbstractEntityRest.getById` from the same DAO calls that fill
 * the `UserAccess` of the layout driven pages. An entity that doesn't carry them says nothing, which is
 * read as "allowed" here, exactly as before these flags existed; a list row carries none either, since
 * only the edit page is asked for.
 *
 * A hint for the UI in every case, never an authorization: the DAO stays the authority, and a client
 * that offers a button anyway gets an `AccessException` from the write it triggers — answered as HTTP
 * 406 with a fieldless validation error (`AbstractPagesRestUtils.handleException`) and shown as a
 * toast. What this decides is only whether the user is offered a button that is bound to fail.
 */

/** The access flags an entity DTO may carry. All optional — most entities carry none. */
export interface EntityAccessFlags {
  writeAccess?: boolean;
  deleteAccess?: boolean;
}

export interface EntityAccess {
  /** Whether the form may be saved. */
  write: boolean;
  /** Whether the entry may be marked as deleted. */
  delete: boolean;
}

/**
 * @param data The entity as the backend delivered it, or the preset of a new one. Typed as `unknown`
 * rather than as [EntityAccessFlags]: the caller is the generic edit page, whose entity type is only
 * known to be `{ id }` — the flags are an *optional* addition, so a type that named them would exclude
 * every entity that has none (TypeScript's weak type detection rejects an object with no property in
 * common).
 * @param isNew True while adding an entry: there is nothing to update or delete yet, and whether it
 * may be inserted is not a flag of the entity but of the list (`userAccess.insert`) — the add page is
 * only reachable from there, so it is not asked again here. Mirrors
 * `AbstractEditForm.updateButtonVisibility`, which shows create-or-update, never both.
 */
export function entityAccess(data: unknown, isNew: boolean): EntityAccess {
  if (isNew) return { write: true, delete: false };
  const flags = (data ?? {}) as EntityAccessFlags;
  return {
    write: flags.writeAccess !== false,
    delete: flags.deleteAccess !== false,
  };
}
