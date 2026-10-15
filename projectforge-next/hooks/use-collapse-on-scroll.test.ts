import { describe, expect, it } from "vitest";
import { LOGO_ROW_HEIGHT, nextCollapsed } from "./use-collapse-on-scroll";

/**
 * Only the decision, not the hook: the wiring and the animation are Playwright's half (see
 * e2e/logo-row.spec.ts and vitest.config.mts). What is worth pinning here is the arithmetic, because
 * both of its failure modes are ones a browser shows only intermittently — a row that flutters at the
 * threshold, and a short column that throws the user's scroll away the moment the row collapses.
 */
describe("nextCollapsed", () => {
  /** A column with plenty of room to collapse, so only the thresholds are in play. */
  const long = { scrollHeight: 5000, clientHeight: 500 };

  it("collapses only past the threshold while expanded", () => {
    expect(nextCollapsed({ ...long, scrollTop: 23 }, false)).toBe(false);
    expect(nextCollapsed({ ...long, scrollTop: 25 }, false)).toBe(true);
  });

  it("expands only back at the very top", () => {
    // The gap between the two thresholds is the hysteresis: between 5 and 24 the state simply holds,
    // whichever it currently is, so resting there cannot make the row flutter.
    expect(nextCollapsed({ ...long, scrollTop: 5 }, true)).toBe(true);
    expect(nextCollapsed({ ...long, scrollTop: 3 }, true)).toBe(false);
    expect(nextCollapsed({ ...long, scrollTop: 20 }, true)).toBe(true);
    expect(nextCollapsed({ ...long, scrollTop: 20 }, false)).toBe(false);
  });

  it("leaves a column alone that has no room to collapse", () => {
    // Overflowing by 50px: collapsing would free 48px, the browser would clamp scrollTop to 2, and
    // the row would expand again - one jump, and the scroll gone.
    const short = { scrollHeight: 550, clientHeight: 500 };
    expect(nextCollapsed({ ...short, scrollTop: 50 }, false)).toBe(false);
    // Just enough room, and it collapses as usual.
    const enough = {
      scrollHeight: 500 + LOGO_ROW_HEIGHT + 25,
      clientHeight: 500,
    };
    expect(nextCollapsed({ ...enough, scrollTop: 50 }, false)).toBe(true);
  });

  it("expands a column that cannot scroll at all", () => {
    expect(
      nextCollapsed(
        { scrollTop: 0, scrollHeight: 500, clientHeight: 500 },
        true
      )
    ).toBe(false);
  });
});
