import { describe, expect, it } from "vitest";
import {
  LOG_LEVEL_KEYS,
  LOG_THRESHOLDS,
  logLevelRowClass,
  logLevelTone,
} from "./log-level";

describe("log level", () => {
  it("offers the thresholds highest first, without FATAL", () => {
    expect(LOG_THRESHOLDS).toEqual(["ERROR", "WARN", "INFO", "DEBUG", "TRACE"]);
  });

  it("shows FATAL as an error", () => {
    expect(LOG_LEVEL_KEYS.FATAL).toBe("log.level.error");
    expect(logLevelTone("FATAL")).toBe("danger");
  });

  it("tints only error rows", () => {
    expect(logLevelRowClass("ERROR")).toBe("row-red");
    expect(logLevelRowClass("FATAL")).toBe("row-red");
    expect(logLevelRowClass("WARN")).toBeUndefined();
    expect(logLevelRowClass("INFO")).toBeUndefined();
  });

  it("tints warnings, keeps info and below neutral", () => {
    expect(logLevelTone("WARN")).toBe("info");
    expect(logLevelTone("INFO")).toBe("neutral");
    expect(logLevelTone("DEBUG")).toBe("neutral");
    expect(logLevelTone("TRACE")).toBe("neutral");
  });
});
