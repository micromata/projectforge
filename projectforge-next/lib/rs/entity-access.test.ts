import { describe, expect, it } from "vitest";
import { entityAccess } from "./entity-access";

/**
 * What the edit page may offer for one entry.
 *
 * The case worth pinning down is the deleted entry: it used to be indistinguishable from a writable
 * one here, so its form offered a save and a second "mark as deleted" — and the save silently brought
 * the entry back (`CandHMaster.copyValues` copies `deleted` from the posted object).
 */
describe("entityAccess", () => {
  it("lets a new entry be saved and has nothing to delete yet", () => {
    expect(entityAccess({ id: null }, true)).toEqual({
      write: true,
      delete: false,
      deleted: false,
    });
  });

  it("reads an entity without flags as writable, as before the flags existed", () => {
    expect(entityAccess({ id: 42 }, false)).toEqual({
      write: true,
      delete: true,
      deleted: false,
    });
  });

  it("follows the flags the entity carries", () => {
    expect(entityAccess({ id: 42, writeAccess: false }, false)).toMatchObject({
      write: false,
      delete: true,
    });
    expect(entityAccess({ id: 42, deleteAccess: false }, false)).toMatchObject({
      write: true,
      delete: false,
    });
  });

  it("leaves a deleted entry nothing but the restore", () => {
    expect(
      entityAccess(
        { id: 42, deleted: true, writeAccess: true, deleteAccess: true },
        false
      )
    ).toEqual({ write: false, delete: false, deleted: true });
  });
});
