import { describe, expect, it } from "vitest";
import { BOOK_METADATA } from "./book.generated";

/**
 * Not generated, on purpose: this is where a change to BookDO becomes loud on the frontend side.
 * `GenerateNextFieldMetadataTest` already guarantees that the file matches the entity, so a failure
 * here means the entity itself changed — a column got shorter, a field became mandatory, an enum
 * gained a constant — and the form that relies on it wants a look.
 *
 * Everything else about the metadata is checked structurally by `tsc` (`as const satisfies
 * EntityMetadata`), so only the values a form actually depends on are spelled out.
 */
describe("BOOK_METADATA", () => {
  it("declares title as mandatory with the length of its column", () => {
    expect(BOOK_METADATA.fields.title.required).toBe(true);
    expect(BOOK_METADATA.fields.title.maxLength).toBe(255);
  });

  it("declares status mandatory and type optional, which is what makes type clearable", () => {
    expect(BOOK_METADATA.fields.status.required).toBe(true);
    expect(BOOK_METADATA.fields.type.required).toBe(false);
  });

  it("does not declare authors mandatory — the server accepts a book without one", () => {
    expect(BOOK_METADATA.fields.authors.required).toBe(false);
    expect(BOOK_METADATA.fields.authors.maxLength).toBe(1000);
  });

  it("carries every constant of BookType and BookStatus with its label key", () => {
    expect(BOOK_METADATA.fields.type.enumValues).toHaveLength(11);
    expect(BOOK_METADATA.fields.status.enumValues).toHaveLength(4);
    expect(BOOK_METADATA.fields.status.enumValues[0]).toEqual({
      value: "PRESENT",
      i18nKey: "book.status.present",
    });
  });

  it("gives no maxLength to a non-string field, whose @Column(length) is a default", () => {
    // 20 in the database (the storage size of the constant's name), no limit for the user.
    expect(
      (BOOK_METADATA.fields.status as { maxLength?: number }).maxLength
    ).toBeUndefined();
    expect(
      (BOOK_METADATA.fields.lendOutDate as { maxLength?: number }).maxLength
    ).toBeUndefined();
  });
});
