import { describe, expect, it } from "vitest";
import type { FormatContext } from "./format";
import {
  formatNumberInput,
  parseNumberInput,
  parsePercentInput,
} from "./number-parse";

/** A German account, as the local test account is: "," decimal, "." group. */
const DE: FormatContext = {
  locale: "de-DE",
  timeZone: "Europe/Berlin",
  currency: "EUR",
};

/** An English account, where both separators are the other way round. */
const EN: FormatContext = { locale: "en-US", currency: "USD" };

describe("formatNumberInput", () => {
  it("writes the separator of the user's locale, not the runtime's", () => {
    expect(formatNumberInput(2394.5, DE, 2)).toBe("2394,50");
    expect(formatNumberInput(2394.5, EN, 2)).toBe("2394.50");
  });

  it("groups thousands only when asked, which is the form a box shows at rest", () => {
    expect(formatNumberInput(2394, DE, 2, true)).toBe("2.394,00");
    expect(formatNumberInput(2394, EN, 2, true)).toBe("2,394.00");
    // Under the caret the separators would move it, so a focused box asks for none.
    expect(formatNumberInput(2394, DE, 2, false)).toBe("2394,00");
  });

  it("pads to the digits an amount has, and keeps what a quantity has without them", () => {
    expect(formatNumberInput(1000, DE, 2, true)).toBe("1.000,00");
    expect(formatNumberInput(0.5, DE, undefined, true)).toBe("0,5");
  });

  it("is empty for no value, which is how the backend stores one", () => {
    expect(formatNumberInput(null, DE, 2, true)).toBe("");
    expect(formatNumberInput(undefined, DE, 2, true)).toBe("");
    expect(formatNumberInput(Number.NaN, DE, 2, true)).toBe("");
  });
});

describe("parseNumberInput", () => {
  it("reads back what a grouped box writes, so a blurred value survives a re-read", () => {
    expect(parseNumberInput(formatNumberInput(2394.5, DE, 2, true), DE)).toBe(
      2394.5
    );
    expect(parseNumberInput(formatNumberInput(2394.5, EN, 2, true), EN)).toBe(
      2394.5
    );
  });

  it("reads a text holding both separators by the locale's own rules", () => {
    expect(parseNumberInput("1.234,56", DE)).toBe(1234.56);
    expect(parseNumberInput("1,234.56", EN)).toBe(1234.56);
  });

  it("takes the other separator as the decimal point when the locale's is absent", () => {
    // The numeric keypad of a German keyboard types ".", and refusing it would be an input that
    // refuses the obvious.
    expect(parseNumberInput("1500.50", DE)).toBe(1500.5);
    expect(parseNumberInput("1500,50", EN)).toBe(1500.5);
  });

  it("takes only the last of several as the decimal point, the rest grouping", () => {
    // "1.234.5" on a German account: two dots, and only the last one can be a decimal point.
    expect(parseNumberInput("1.234.5", DE)).toBe(1234.5);
    expect(parseNumberInput("1,234,5", EN)).toBe(1234.5);
  });

  it("is null for what is not a number yet, so nothing is lost while typing", () => {
    expect(parseNumberInput("", DE)).toBeNull();
    expect(parseNumberInput("   ", DE)).toBeNull();
    expect(parseNumberInput("abc", DE)).toBeNull();
    expect(parseNumberInput("-", DE)).toBeNull();
  });

  it("keeps a negative amount negative", () => {
    expect(parseNumberInput("-1.234,56", DE)).toBe(-1234.56);
  });
});

describe("parsePercentInput", () => {
  it("reads the percentage a trailing sign asks for, spaced or not", () => {
    expect(parsePercentInput("50%", DE)).toBe(50);
    expect(parsePercentInput(" 50 % ", DE)).toBe(50);
    // In the user's own layout, like every other number typed into a box.
    expect(parsePercentInput("33,33%", DE)).toBe(33.33);
    expect(parsePercentInput("33.33%", EN)).toBe(33.33);
  });

  it("is null for a plain number, which is an amount and not a share", () => {
    expect(parsePercentInput("50", DE)).toBeNull();
    expect(parsePercentInput("", DE)).toBeNull();
    // A sign without digits is not a percentage yet — nothing to take a share of.
    expect(parsePercentInput("%", DE)).toBeNull();
  });

  it("takes only a trailing sign, so a pasted amount stays an amount", () => {
    expect(parsePercentInput("%50", DE)).toBeNull();
  });

  it("keeps a negative share negative, as a correcting row is entered", () => {
    expect(parsePercentInput("-10%", DE)).toBe(-10);
  });
});
