import { describe, expect, it } from "vitest";
import { emptyAccountValues, toFormValues } from "./account-values";

describe("toFormValues", () => {
  // The form's values are posted as the DTO: without the id, saving an existing account was an insert.
  it("keeps the id of a saved account, so saving it updates it", () => {
    expect(toFormValues({ id: 7, nummer: 4711 }).id).toBe(7);
  });

  it("has no id for a new account", () => {
    expect(emptyAccountValues().id).toBeNull();
  });
});
