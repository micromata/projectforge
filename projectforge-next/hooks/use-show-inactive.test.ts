import { describe, expect, it } from "vitest";
import { autosearchEntity } from "./use-show-inactive";

describe("autosearchEntity", () => {
  it("takes the category of an autosearch url", () => {
    expect(autosearchEntity("user/autosearch?search=:search")).toBe("user");
    expect(
      autosearchEntity("/cost2/autosearch?projektId=1&search=:search")
    ).toBe("cost2");
  });

  it("is null for any other url", () => {
    expect(autosearchEntity("vacation/users?search=:search")).toBeNull();
    expect(autosearchEntity("task/autosearchTree")).toBeNull();
  });
});
