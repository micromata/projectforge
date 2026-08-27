import { describe, expect, it } from "vitest";
import { toTeamEventRoute } from "./team-event-route";

describe("toTeamEventRoute", () => {
  it("maps the new-event url and keeps its preset query", () => {
    expect(
      toTeamEventRoute("/teamEvent/edit?startDate=1&endDate=2&calendar=3")
    ).toBe("/teamEvent/new?startDate=1&endDate=2&calendar=3");
    expect(toTeamEventRoute("/teamEvent/edit")).toBe("/teamEvent/new");
  });

  it("maps an existing event by id and keeps its moved-position query", () => {
    expect(toTeamEventRoute("/teamEvent/edit/7")).toBe("/teamEvent/7");
    // A drag/resize carries the new period and, for a series, the moved occurrence's origin — which
    // the edit target turns into the move to save (see calendar-edit-target).
    expect(
      toTeamEventRoute(
        "/teamEvent/edit/7?startDate=1&endDate=2&origStartDate=3&origEndDate=4"
      )
    ).toBe("/teamEvent/7?startDate=1&endDate=2&origStartDate=3&origEndDate=4");
  });

  it("leaves a timesheet or otherwise unrelated url untouched", () => {
    expect(toTeamEventRoute("/timesheet/edit/42")).toBe("/timesheet/edit/42");
    expect(toTeamEventRoute("/calendar")).toBe("/calendar");
    expect(toTeamEventRoute("https://example.com/teamEvent/edit/1")).toBe(
      "https://example.com/teamEvent/edit/1"
    );
  });

  it("does not match a deeper path than a plain id", () => {
    expect(toTeamEventRoute("/teamEvent/edit/7/clone")).toBe(
      "/teamEvent/edit/7/clone"
    );
    expect(toTeamEventRoute("/teamEvent/edit/")).toBe("/teamEvent/edit/");
  });
});
