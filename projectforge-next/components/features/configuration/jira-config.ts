/**
 * The JSON value of the configuration parameter `jira`, mirroring `JiraConfig` (projectforge-business): the
 * default browse url issue keys are appended to and further servers by the projects hosted on them. The
 * backend validates it on saving.
 */
export const JIRA_PARAM = "jira";

export interface JiraServerEntry {
  /** Url the issue key is appended to, e.g. `https://customer.acme.com/jira/browse/`. */
  baseUrl: string;
  /** Keys of the projects hosted on the server, comma separated, e.g. `ACME, ADMIN`. */
  projects: string;
}

export interface JiraConfigValue {
  /** Url of the issues of all projects not hosted on one of the servers. */
  browseBaseUrl: string;
  servers: JiraServerEntry[];
}

export const EMPTY_JIRA_SERVER: JiraServerEntry = { baseUrl: "", projects: "" };

const EMPTY_CONFIG: JiraConfigValue = { browseBaseUrl: "", servers: [] };

/** The settings of the stored value, none for an empty one. `invalid` if the value isn't a JSON object. */
export function parseJiraConfig(json: string | null | undefined): {
  config: JiraConfigValue;
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { config: EMPTY_CONFIG, invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
      return { config: EMPTY_CONFIG, invalid: true };
    }
    const record = value as Record<string, unknown>;
    const servers = Array.isArray(record.servers) ? record.servers : [];
    return {
      config: {
        browseBaseUrl: typeof record.browseBaseUrl === "string" ? record.browseBaseUrl : "",
        servers: servers
          .filter(
            (server): server is Record<string, unknown> =>
              typeof server === "object" && server !== null
          )
          .map((server) => ({
            baseUrl: typeof server.baseUrl === "string" ? server.baseUrl : "",
            projects: typeof server.projects === "string" ? server.projects : "",
          })),
      },
      invalid: false,
    };
  } catch {
    return { config: EMPTY_CONFIG, invalid: true };
  }
}

/** The project keys of a server as the backend reads them (split by comma or semicolon, upper case). */
export function jiraProjectKeys(projects: string): string[] {
  return projects
    .split(/[,;]/)
    .map((project) => project.trim().toUpperCase())
    .filter((project) => project.length > 0);
}

/**
 * The JSON value of the settings, empty if there is neither a default url nor a server. Not trimmed: it's
 * written on every keystroke, the backend trims.
 */
export function serializeJiraConfig(config: JiraConfigValue): string {
  if (config.browseBaseUrl.length === 0 && config.servers.length === 0) {
    return "";
  }
  return JSON.stringify({
    browseBaseUrl: config.browseBaseUrl,
    servers: config.servers.map((server) => ({
      baseUrl: server.baseUrl,
      projects: server.projects,
    })),
  });
}
