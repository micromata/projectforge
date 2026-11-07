import { describe, expect, it } from "vitest";
import { badgeKey } from "./use-app-status-signal";

/** The app is told of a change of what its badges show, and of nothing else. */
describe("badgeKey", () => {
  const status = {
    chatUnread: 2,
    notifications: { openCount: 3, latestId: 17 },
  };

  it("changes with the unread chat messages, the open notifications and the newest one", () => {
    expect(badgeKey({ ...status, chatUnread: 0 })).not.toBe(badgeKey(status));
    expect(
      badgeKey({ ...status, notifications: { openCount: 2, latestId: 17 } })
    ).not.toBe(badgeKey(status));
    expect(
      badgeKey({ ...status, notifications: { openCount: 3, latestId: 18 } })
    ).not.toBe(badgeKey(status));
  });

  it("stays without notifications loaded", () => {
    expect(badgeKey({ chatUnread: 0, notifications: null })).toBe(
      badgeKey({ chatUnread: 0, notifications: null })
    );
  });
});
