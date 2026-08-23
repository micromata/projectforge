import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

/**
 * Credentials of the local test accounts, read from `$PROJECTFORGE_HOME/testAccounts.txt`
 * (one line per role, `role=username/password`).
 *
 * The file is written by the running instance itself: in development mode ProjectForge creates the
 * four `e2e-*` accounts it doesn't find and notes their generated random passwords there
 * (`E2ETestAccountsService`). Nothing to set up by hand, and nothing that could be committed — the
 * accounts belong to one developer's local instance, and hard coding them would make the tests
 * unrunnable for everyone else. See projectforge-next/CLAUDE.md.
 */
export interface Credentials {
  username: string;
  password: string;
}

/**
 * The roles a spec may ask for. One account cannot carry them all, and that is the point: with only
 * the full-access one, every rights rule is testable in its success half alone — the refusal, which
 * is the half that matters, is unreachable (see MIGRATION.md, "Mehrere Testkonten statt einem").
 */
export type Role =
  /** Every group and every right: sees everything, may write everything. */
  | "full-access-user"
  /** The finance rights without the admin group. */
  | "finance-user"
  /** The admin group without the finance rights — refused on invoices and the order book. */
  | "admin-user"
  /** A logged in user with no special rights, and `locale=en`. */
  | "normalo-user";

/** What a spec gets when it asks for no particular role. */
export const DEFAULT_ROLE: Role = "full-access-user";

const HOME_DIR =
  process.env.PROJECTFORGE_HOME ?? join(homedir(), "ProjectForge");

/** The generated file, or — on an instance that still has one — the hand-written predecessor. */
const CREDENTIALS_FILE =
  [join(HOME_DIR, "testAccounts.txt"), join(HOME_DIR, "testAccount.txt")].find(
    existsSync
  ) ?? join(HOME_DIR, "testAccounts.txt");

/**
 * @param role The account to use; omit it for the full-access one.
 * @throws Error if the file is missing or holds no line for `role`. A spec that needs a role the
 *   local instance has no account for should skip rather than fail — see [hasRole].
 */
export function readCredentials(role: Role = DEFAULT_ROLE): Credentials {
  const accounts = readAccounts();
  const found = accounts.get(role);
  if (!found) {
    throw new Error(
      `${CREDENTIALS_FILE} holds no account for the role "${role}". Restarting ProjectForge in ` +
        `development mode writes the missing line; add it by hand to point the role at an account ` +
        `of your own (see CLAUDE.md, "Testing against the running system").`
    );
  }
  return found;
}

/** Whether the file names an account for `role`, so a spec can skip instead of failing. */
export function hasRole(role: Role): boolean {
  try {
    return readAccounts().has(role);
  } catch {
    return false;
  }
}

/**
 * Parses the whole file: `role=username/password` per line, `#` comments and blank lines skipped.
 *
 * A line without a role is taken as the full-access account, which is the format the file had when
 * there was only one — so an instance whose file was never extended keeps working.
 */
function readAccounts(): Map<Role, Credentials> {
  let raw: string;
  try {
    raw = readFileSync(CREDENTIALS_FILE, "utf8");
  } catch {
    throw new Error(
      `No test accounts found at ${CREDENTIALS_FILE}. ProjectForge writes that file on every ` +
        `start when it runs with projectforge.development.mode=true — set it in ` +
        `$PROJECTFORGE_HOME/projectforge.properties and restart ` +
        `(see CLAUDE.md, "Testing against the running system").`
    );
  }
  const accounts = new Map<Role, Credentials>();
  for (const line of raw.split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const equals = trimmed.indexOf("=");
    const role = equals > 0 ? (trimmed.slice(0, equals) as Role) : DEFAULT_ROLE;
    const account = trimmed.slice(equals + 1);
    // The password may contain slashes and even an `=`; the username may not, so both are split at
    // their first occurrence only.
    const slash = account.indexOf("/");
    if (slash <= 0 || slash === account.length - 1) {
      // The offending line is deliberately not part of the message: it is the line that holds the
      // password, and a failing test's output ends up in a log or a terminal recording.
      throw new Error(
        `Every line of ${CREDENTIALS_FILE} must read "role=username/password".`
      );
    }
    accounts.set(role, {
      username: account.slice(0, slash),
      password: account.slice(slash + 1),
    });
  }
  return accounts;
}
