import { describe, expect, it } from "vitest";
import { secretPeek } from "./secret-peek";

describe("secretPeek", () => {
  it("shows a third of a short secret", () => {
    expect(secretPeek("abcdef")).toBe("ab");
  });

  it("shows at most four characters of a long one", () => {
    expect(secretPeek("sePs-i3fx-Qw7p-0aZk")).toBe("sePs");
  });

  it("shows nothing of a secret too short to give anything away", () => {
    expect(secretPeek("ab")).toBe("");
    expect(secretPeek("")).toBe("");
  });
});
