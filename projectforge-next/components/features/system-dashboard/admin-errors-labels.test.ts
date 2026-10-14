import { describe, expect, it } from "vitest";
import { statusFilterAfter } from "./admin-errors-labels";

describe("statusFilterAfter", () => {
  it("points to the status a problem left the list for", () => {
    expect(statusFilterAfter("OPEN", "RESOLVE")).toBe("RESOLVED");
    expect(statusFilterAfter("OPEN", "IGNORE")).toBe("IGNORED");
    expect(statusFilterAfter("NEW", "ACKNOWLEDGE")).toBe("ACKNOWLEDGED");
    expect(statusFilterAfter("RESOLVED", "REOPEN")).toBe("NEW");
    expect(statusFilterAfter("MUTED", "RESOLVE")).toBe("RESOLVED");
  });

  it("is null while the filter still lists the problem", () => {
    expect(statusFilterAfter("ALL", "RESOLVE")).toBeNull();
    expect(statusFilterAfter("OPEN", "ACKNOWLEDGE")).toBeNull();
    expect(statusFilterAfter("MUTED", "REOPEN")).toBeNull();
    expect(statusFilterAfter("RESOLVED", "RESOLVE")).toBeNull();
    expect(statusFilterAfter("OPEN", "MUTE")).toBeNull();
    expect(statusFilterAfter("OPEN", "SET_NOTIFY")).toBeNull();
  });

  it("lists an unmuted problem as open, if the muted ones were shown", () => {
    expect(statusFilterAfter("MUTED", "UNMUTE")).toBe("OPEN");
    expect(statusFilterAfter("OPEN", "UNMUTE")).toBeNull();
  });
});
