import { readFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

/**
 * Credentials of the local test account, read from `~/ProjectForge/testAccount.txt`
 * (one line, `username/password`).
 *
 * Deliberately a file outside the repository: the account belongs to the developer's local
 * ProjectForge instance, so it must never be committed, and hard coding it would make the tests
 * unrunnable for everyone else. See projectforge-next/CLAUDE.md.
 */
export interface Credentials {
  username: string;
  password: string;
}

const CREDENTIALS_FILE = join(homedir(), "ProjectForge", "testAccount.txt");

export function readCredentials(): Credentials {
  let raw: string;
  try {
    raw = readFileSync(CREDENTIALS_FILE, "utf8");
  } catch {
    throw new Error(
      `No test account found at ${CREDENTIALS_FILE}. Create it with one line "username/password" ` +
        `of a local ProjectForge account (see CLAUDE.md, "Testing against the running system").`
    );
  }
  // Only the first line counts, so the file may carry a comment below it.
  const line = raw.split("\n")[0]?.trim() ?? "";
  // The password may contain slashes; the username may not, so split at the first one only.
  const slash = line.indexOf("/");
  if (slash <= 0 || slash === line.length - 1) {
    // The offending line is deliberately not part of the message: it is the line that holds the
    // password, and a failing test's output ends up in a log or a terminal recording.
    throw new Error(
      `${CREDENTIALS_FILE} must hold "username/password" on its first line.`
    );
  }
  return {
    username: line.slice(0, slash),
    password: line.slice(slash + 1),
  };
}
