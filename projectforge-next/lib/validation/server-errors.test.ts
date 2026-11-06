import { describe, expect, it } from "vitest";
import { applyServerValidationErrors } from "./server-errors";

/** A stand-in for the form: records the error map the errors are written to. */
function form() {
  const target = {
    errorMap: undefined as unknown,
    setErrorMap(map: unknown) {
      target.errorMap = map;
    },
  };
  return target;
}

describe("applyServerValidationErrors", () => {
  it("assigns the errors of fields the form knows, the others are left for a toast", () => {
    const target = form();
    const result = applyServerValidationErrors(
      target,
      [
        { fieldId: "name", message: "Name is required." },
        { fieldId: "unknown", message: "Somewhere else." },
        { fieldId: "", message: "No field at all." },
      ],
      ["name"]
    );
    expect(result).toEqual({
      unassigned: ["Somewhere else.", "No field at all."],
      hasAssigned: true,
    });
    expect(target.errorMap).toEqual({
      onServer: { fields: { name: "Name is required." } },
    });
  });

  it("knows the fields of a group declared as `group.*`", () => {
    const target = form();
    const result = applyServerValidationErrors(
      target,
      [
        { fieldId: "params.cvrCaseId", message: "The case is closed." },
        { fieldId: "paramsX", message: "Not of the group." },
      ],
      ["name", "params.*"]
    );
    expect(result.unassigned).toEqual(["Not of the group."]);
    expect(target.errorMap).toEqual({
      onServer: { fields: { "params.cvrCaseId": "The case is closed." } },
    });
  });
});
