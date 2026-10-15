import { describe, expect, it } from "vitest";
import { isSubmitKey, type SubmitShortcutKeys } from "./submit-shortcut";

/**
 * The keys of the shortcut, not its target: whether an element is a textarea is a DOM question and
 * these tests run without one (see vitest.config.mts), so [isSubmitKey] takes that answer as a flag.
 */
function keys(overrides: Partial<SubmitShortcutKeys> = {}): SubmitShortcutKeys {
  return {
    key: "Enter",
    ctrlKey: false,
    metaKey: false,
    altKey: false,
    shiftKey: false,
    defaultPrevented: false,
    ...overrides,
  };
}

describe("isSubmitKey", () => {
  it("submits on a bare Return in a single-line field", () => {
    expect(isSubmitKey(keys(), false)).toBe(true);
  });

  it("leaves a bare Return in a textarea alone — it is the line break there", () => {
    expect(isSubmitKey(keys(), true)).toBe(false);
  });

  it.each([
    ["CTRL", { ctrlKey: true }],
    ["CMD", { metaKey: true }],
  ])("submits on %s-Return in a textarea", (_name, modifier) => {
    expect(isSubmitKey(keys(modifier), true)).toBe(true);
  });

  it("submits on CTRL-Return in a single-line field too", () => {
    expect(isSubmitKey(keys({ ctrlKey: true }), false)).toBe(true);
  });

  it("ignores CTRL and CMD held together", () => {
    expect(isSubmitKey(keys({ ctrlKey: true, metaKey: true }), true)).toBe(
      false
    );
  });

  it.each(["altKey", "shiftKey"] as const)("ignores %s", (modifier) => {
    expect(isSubmitKey(keys({ [modifier]: true }), false)).toBe(false);
    expect(isSubmitKey(keys({ [modifier]: true, ctrlKey: true }), true)).toBe(
      false
    );
  });

  it("leaves a Return another handler already claimed alone", () => {
    // What keeps DateInput, TimeInput and TagInput working: they commit on Return and preventDefault.
    expect(isSubmitKey(keys({ defaultPrevented: true }), false)).toBe(false);
  });

  it.each([
    ["the event itself", { isComposing: true }],
    ["the native event", { nativeEvent: { isComposing: true } }],
  ])("ignores a Return that ends an IME composition (%s)", (_where, event) => {
    expect(isSubmitKey(keys(event), false)).toBe(false);
  });

  it("ignores any other key", () => {
    expect(isSubmitKey(keys({ key: "Escape" }), false)).toBe(false);
    expect(isSubmitKey(keys({ key: "a", ctrlKey: true }), false)).toBe(false);
  });
});
