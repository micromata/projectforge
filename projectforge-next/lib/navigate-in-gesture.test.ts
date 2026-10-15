import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BASE_PATH } from "./config";
import { navigateInGesture } from "./navigate-in-gesture";

const pushState = vi.fn();
const router = { push: vi.fn(), replace: vi.fn() };
const state = { __NA: true, __PRIVATE_NEXTJS_INTERNALS_TREE: {} };

function openAt(pathname: string, search = "") {
  vi.stubGlobal("window", {
    location: { pathname, search, hash: "" },
    history: { state, pushState },
  });
}

describe("navigateInGesture", () => {
  beforeEach(() => openAt(`${BASE_PATH}/accounting-record/`));
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.clearAllMocks();
  });

  it("pushes the entry itself, then lets the router replace into it", () => {
    navigateInGesture(router, "/accounting-record/42?returnTo=/x");
    expect(pushState).toHaveBeenCalledExactlyOnceWith(
      state,
      "",
      `${BASE_PATH}/accounting-record/42?returnTo=/x`
    );
    expect(router.replace).toHaveBeenCalledExactlyOnceWith(
      "/accounting-record/42?returnTo=/x",
      undefined
    );
    expect(pushState.mock.invocationCallOrder[0]).toBeLessThan(
      router.replace.mock.invocationCallOrder[0]
    );
    expect(router.push).not.toHaveBeenCalled();
  });

  it("adds no entry for the page that is open, trailing slash or not", () => {
    navigateInGesture(router, "/accounting-record");
    expect(pushState).not.toHaveBeenCalled();
    expect(router.replace).toHaveBeenCalledOnce();

    openAt(`${BASE_PATH}/order/7/`, "?tab=forecast");
    navigateInGesture(router, "/order/7?tab=forecast");
    expect(pushState).not.toHaveBeenCalled();
  });

  it("adds an entry when only the search part differs", () => {
    openAt(`${BASE_PATH}/order/7/`, "?tab=forecast");
    navigateInGesture(router, "/order/7?tab=history");
    expect(pushState).toHaveBeenCalledOnce();
  });

  it("leaves a relative href to the router's push", () => {
    navigateInGesture(router, "edit", { scroll: false });
    expect(pushState).not.toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledExactlyOnceWith("edit", {
      scroll: false,
    });
  });
});
