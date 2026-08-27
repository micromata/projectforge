import { describe, expect, it } from "vitest";
import { toTimesheetRoute } from "./timesheet-route";

describe("toTimesheetRoute", () => {
  it("maps the new-sheet url and keeps its preset query", () => {
    expect(toTimesheetRoute("/timesheet/edit?startDate=1&endDate=2")).toBe(
      "/timesheet/new?startDate=1&endDate=2"
    );
    expect(toTimesheetRoute("/timesheet/edit")).toBe("/timesheet/new");
  });

  it("maps an existing sheet by id and keeps its moved-position query", () => {
    expect(toTimesheetRoute("/timesheet/edit/42")).toBe("/timesheet/42");
    // A drag/resize carries the new start and end, which the edit target turns into the move to save.
    expect(toTimesheetRoute("/timesheet/edit/42?startDate=1&endDate=2")).toBe(
      "/timesheet/42?startDate=1&endDate=2"
    );
  });

  it("leaves a team-event or otherwise unrelated url untouched", () => {
    expect(toTimesheetRoute("/teamEvent/edit/7")).toBe("/teamEvent/edit/7");
    expect(toTimesheetRoute("/calendar")).toBe("/calendar");
    expect(toTimesheetRoute("https://example.com/timesheet/edit/1")).toBe(
      "https://example.com/timesheet/edit/1"
    );
  });

  it("does not match a deeper path than a plain id", () => {
    expect(toTimesheetRoute("/timesheet/edit/42/clone")).toBe(
      "/timesheet/edit/42/clone"
    );
    expect(toTimesheetRoute("/timesheet/edit/")).toBe("/timesheet/edit/");
  });
});
