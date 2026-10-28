import { describe, expect, it } from "vitest";
import { jiraProjectKeys, parseJiraConfig, serializeJiraConfig } from "./jira-config";

const STORED =
  '{"browseBaseUrl":"https://jira.acme.com/jira/browse/","servers":[{"baseUrl":"https://customer.acme.com/browse/","projects":"ACME; admin"}]}';

describe("jira config", () => {
  it("reads and writes the stored value unchanged", () => {
    const { config, invalid } = parseJiraConfig(STORED);
    expect(invalid).toBe(false);
    expect(config).toEqual({
      browseBaseUrl: "https://jira.acme.com/jira/browse/",
      servers: [{ baseUrl: "https://customer.acme.com/browse/", projects: "ACME; admin" }],
    });
    expect(serializeJiraConfig(config)).toBe(STORED);
    expect(jiraProjectKeys(config.servers[0].projects)).toEqual(["ACME", "ADMIN"]);
  });

  it("handles empty and invalid values", () => {
    expect(parseJiraConfig(null)).toEqual({
      config: { browseBaseUrl: "", servers: [] },
      invalid: false,
    });
    expect(parseJiraConfig("[]").invalid).toBe(true);
    expect(parseJiraConfig("{").invalid).toBe(true);
    expect(serializeJiraConfig({ browseBaseUrl: "", servers: [] })).toBe("");
  });
});
