import type { APIRequestContext, Page } from "@playwright/test";

/**
 * Test data the specs create for themselves, through the REST API.
 *
 * Two reasons this exists, and the first one is the important one:
 *
 * - **The database is a copy of production.** A spec that names a row of it — a book title, a
 *   customer, a cost unit number — copies confidential content into a public repository (the remote
 *   is github.com/micromata/projectforge). So no spec may carry business content of the database,
 *   and none may need a particular row to be there.
 * - **A spec has to pass on a fresh database too.** An assertion on "the first row" or on a count is
 *   a statement about a dump that nobody else has, and on an empty schema it fails for a reason that
 *   says nothing about the code.
 *
 * What a spec needs, it therefore creates. Everything created carries [MARKER] in a text field, so a
 * row is recognizable as the tests' own — and everything unique is derived from [uniqueSuffix], so a
 * second run does not collide with the first.
 *
 * Nothing is deleted again. The database is expendable (a test instance, not the production one),
 * and the entities in question cannot be removed cleanly anyway: `BookDO`, `AuftragDO` and `TaskDO`
 * are historizable, so `forceDelete` refuses them and `markAsDeleted` merely sets a flag; an
 * inserted `Kost1DO` occupies its number for good (`Kost1Dao.onInsertOrModify` collides with a
 * deleted row just as with a live one). What matters is that a *following* run still passes, which
 * the unique suffix and the `deleted=false` default of every list filter take care of.
 */

/** In a title or description of everything these tests insert, so a row is recognizable as theirs. */
export const MARKER = "ZZ e2e";

/**
 * Distinguishes the rows of one run from those of the next, for the fields with a unique constraint
 * (a book's signature, an order's title, a cost unit's number).
 *
 * The timestamp in seconds, base 36: short enough to fit into a signature column and monotonic, so
 * the newest row of a search is the current run's. `Math.random` is deliberately not part of it —
 * two runs in the same second would be the same run for a human reading the rows.
 */
export function uniqueSuffix(): string {
  return Math.floor(Date.now() / 1000).toString(36);
}

/** The headers a state changing call needs; the CSRF token is read per call rather than cached. */
async function writeHeaders(
  request: APIRequestContext
): Promise<Record<string, string>> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}

/**
 * Inserts an entity through its page's `saveorupdate` and answers with the new id.
 *
 * The endpoint speaks `PostData`/`ResponseAction` (see lib/rs/entity.ts), so the id arrives as
 * `variables.id` and not as the saved entity. An HTTP 406 carries `validationErrors` — thrown here
 * with the backend's own message, because a seed that fails silently turns into a spec failing much
 * later on something unrelated.
 */
export async function insert(
  request: APIRequestContext,
  entity: string,
  data: Record<string, unknown>
): Promise<number> {
  const res = await request.put(`/rs/${entity}/saveorupdate`, {
    headers: await writeHeaders(request),
    data: { data },
  });
  const body = (await res.json()) as {
    variables?: { id?: number };
    validationErrors?: { message?: string }[];
  };
  if (!res.ok() || body.variables?.id == null) {
    const reason =
      body.validationErrors?.map((e) => e.message).join("; ") ??
      `HTTP ${res.status()}`;
    throw new Error(`Could not create a ${entity} for the test: ${reason}`);
  }
  return body.variables.id;
}

/** Reads an entity back as its page's DTO — what a write has to be given in full. */
export async function fetchEntity<T>(
  request: APIRequestContext,
  entity: string,
  id: number
): Promise<T> {
  const res = await request.get(`/rs/${entity}/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!res.ok()) {
    throw new Error(`Could not read ${entity} ${id}: HTTP ${res.status()}`);
  }
  return (await res.json()) as T;
}

/** A book of the tests' own, with every field the book specs read set to a known value. */
export interface SeededBook {
  id: number;
  title: string;
  /** Unique per run, so the list can be searched for exactly this book. */
  signature: string;
  /** `BookStatus`, chosen so it differs from the form's default (see book-edit.spec.ts). */
  status: "DISPOSED";
  type: "BOOK";
}

/**
 * Creates a book.
 *
 * The field selection is what the book specs assert on, and it is deliberate:
 * - `status: DISPOSED` differs from the form's default, which is the case book-edit.spec.ts needs to
 *   catch a select that bounces its value back while closed.
 * - `editor` and `lendOutComment` stay unset, so Spring omits them from the JSON
 *   (`JsonInclude.Include.NON_NULL`) — the "field arrives as undefined" case of the same spec.
 * - the book is *not* lent out, so book-lend-out.spec.ts finds the lend-out button.
 */
export async function createBook(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededBook> {
  const title = `${MARKER} book ${suffix}`;
  const signature = `${MARKER}-${suffix}`;
  const id = await insert(request, "book", {
    title,
    signature,
    authors: `${MARKER} author`,
    isbn: `${MARKER}-isbn-${suffix}`,
    status: "DISPOSED",
    type: "BOOK",
  });
  return { id, title, signature, status: "DISPOSED", type: "BOOK" };
}

/**
 * Creates a book and modifies it once, so it has a history whose newest entry is an *update*.
 *
 * History is written as a side effect of saving (`HistoryBaseDaoAdapter`), so the only way to a
 * predictable history is to produce it: the insert brings one attribute per property, the modify one
 * for the changed field. A spec asserting on a history therefore needs no particular row of the
 * database — which it could not have on a fresh one anyway.
 */
export async function createBookWithHistory(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededBook> {
  const book = await createBook(request, suffix);
  // Read back and post the whole DTO: `saveorupdate` saves what it is given, so a partial body would
  // clear every field left out of it — and clearing them would be a change of its own in the history.
  const stored = await fetchEntity<Record<string, unknown>>(
    request,
    "book",
    book.id
  );
  const abstract = `${MARKER} modified ${suffix}`;
  await insert(request, "book", { ...stored, abstractText: abstract });
  return book;
}

/** A cost unit of the tests' own, with the four parts of its number as the form shows them. */
export interface SeededCost1 {
  id: number;
  /** "9.123.45.67", as `KostFormatter` writes it. */
  number: string;
  description: string;
  /** The run's own suffix, a word of the description — the search term that hits this row only. */
  suffix: string;
  /** The four boxes of the segmented field, zero-padded the way the page prefills them. */
  parts: {
    nummernkreis: string;
    bereich: string;
    teilbereich: string;
    endziffer: string;
  };
}

/**
 * Creates a cost unit whose number is free.
 *
 * `nummernkreis` 9 rather than a random digit: the number has to be one that the *production* chart
 * of accounts is unlikely to use, since an inserted cost number can never be released again
 * (`Kost1DO` supports no real delete). The rest of the number comes from the run's suffix, and a
 * collision is answered by trying the next candidate — after 20 attempts something else is wrong.
 *
 * The parts are bounded by their columns: nummernkreis 0-9, bereich 0-999, teilbereich and endziffer
 * 0-99 (`Kost1DO`, checked by `Kost1Dao.verifyKost`).
 */
export async function createCost1(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededCost1> {
  const seed = Math.floor(Date.now() / 1000);
  for (let attempt = 0; attempt < 20; attempt++) {
    const n = seed + attempt;
    const parts = {
      nummernkreis: 9,
      bereich: n % 1000,
      teilbereich: Math.floor(n / 1000) % 100,
      endziffer: Math.floor(n / 100_000) % 100,
    };
    const description = `${MARKER} cost unit ${suffix}`;
    try {
      const id = await insert(request, "cost1", {
        ...parts,
        description,
        kostentraegerStatus: "ACTIVE",
      });
      return {
        id,
        suffix,
        number: [
          parts.nummernkreis,
          String(parts.bereich).padStart(3, "0"),
          String(parts.teilbereich).padStart(2, "0"),
          String(parts.endziffer).padStart(2, "0"),
        ].join("."),
        description,
        parts: {
          nummernkreis: String(parts.nummernkreis),
          bereich: String(parts.bereich).padStart(3, "0"),
          teilbereich: String(parts.teilbereich).padStart(2, "0"),
          endziffer: String(parts.endziffer).padStart(2, "0"),
        },
      };
    } catch (cause) {
      // Only a taken number is worth another attempt; anything else (a missing right, a changed
      // contract) would fail 20 times over and bury its own reason.
      if (!/kost1|bereits|already/i.test(String(cause))) throw cause;
    }
  }
  throw new Error(
    "Could not find a free cost number after 20 attempts — see Kost1Dao.onInsertOrModify."
  );
}

/** An order of the tests' own, whose title is too long for any column to show in full. */
export interface SeededOrder {
  id: number;
  title: string;
}

/**
 * Creates an order with one position and a title far wider than its column.
 *
 * The width is the point: `data-table-overflow-tooltip.spec.ts` needs a cell whose content is
 * clipped, and on an empty database — or a narrow one — no row of the list would provide one. 200
 * characters are well inside the column (`AuftragDO.titel` is 1000) and far beyond any width the
 * table gives it.
 */
export async function createOrder(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededOrder> {
  const title = `${MARKER} order ${suffix} ${"long ".repeat(40)}`.trim();
  // The period is mandatory as soon as a position inherits it, which is the default
  // (`PeriodOfPerformanceValidator`). Fixed dates rather than today's: nothing here depends on when
  // the run happens, and a fixed pair keeps the seeded rows comparable across runs.
  const id = await insert(request, "order", {
    titel: title,
    status: "IN_ERSTELLUNG",
    periodOfPerformanceBegin: "2026-03-01",
    periodOfPerformanceEnd: "2026-06-30",
    positionen: [{ number: 1, status: "IN_ERSTELLUNG", titel: title }],
  });
  return { id, title };
}

/** Read once per worker: the tree does not gain a new root while a run is going on. */
let rootTaskId: number | undefined;

/**
 * Asks the backend for the id of the tree's root, because a task always needs a parent — one without
 * is refused ("task.error.parentTaskNotFound").
 *
 * Not hard coded to 1, although `ProjectForgeRoot` usually has that id: the backend itself says the
 * root's id is "1 only by convention" (`TaskServicesRest.Task.root`), and `hibernate_sequence` hands
 * out ids in steps of 50, so on a database set up differently pk 1 may be an ordinary task — under
 * which these tests would then silently hang their tasks.
 *
 * `TaskServicesRest.getRoot` answers it for every account, which is also where the app itself asks (the
 * wizard's "create structure element" link presets the root as the parent). Before that endpoint existed
 * this walked the tree: `showRootForAdmins` appends the root flagged `root: true`, but only for admins
 * and financial staff, so a plain account had to follow the parent chain of any visible node up to the
 * one task without a parent.
 */
export async function fetchRootTaskId(
  request: APIRequestContext
): Promise<number> {
  if (rootTaskId != null) return rootTaskId;
  const res = await request.get("/rs/task/tree/root", {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!res.ok()) {
    throw new Error(
      `Could not read the task tree's root: HTTP ${res.status()}`
    );
  }
  const { id } = (await res.json()) as { id: number };
  return (rootTaskId = id);
}

/** A task of the tests' own and one child, so the tree has a node that can be expanded. */
export interface SeededTask {
  id: number;
  title: string;
  /**
   * The run's own suffix, and the one word of the title that hits this run only — the rest of it
   * ("ZZ e2e task") is in every task an earlier run left, and the backend's search matches a row on
   * any word of the term.
   */
  suffix: string;
  child: { id: number; title: string };
}

/**
 * Creates a task with one child below the root.
 *
 * The child is the point: task-tree.spec.ts needs a *collapsed folder* — a node with children that
 * is currently closed — and on a database without one the spec would wait for an expand arrow that
 * never appears.
 */
export async function createTask(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededTask> {
  const title = `${MARKER} task ${suffix}`;
  const id = await insert(request, "task", {
    title,
    status: "O",
    parentTask: { id: await fetchRootTaskId(request) },
  });
  const childTitle = `${MARKER} subtask ${suffix}`;
  const childId = await insert(request, "task", {
    title: childTitle,
    status: "O",
    parentTask: { id },
  });
  return { id, title, suffix, child: { id: childId, title: childTitle } };
}

/** A group of the tests' own, for the specs that hand rights to one. */
export interface SeededGroup {
  id: number;
  name: string;
  /** The run's own suffix, the one word of the name that hits this run only. */
  suffix: string;
}

/**
 * Creates a group.
 *
 * `localGroup: true`, so the group is never written to an LDAP the installation may be attached to
 * (`GroupDO.localGroup`) — a test group has no business leaving the database it was made in. No members:
 * the specs that need one grant it rights, they do not log in as it.
 *
 * Created rather than looked up, although the database has hundreds: a spec that grants rights to a
 * group of a production copy changes what real people may see, and it would have to name that group in
 * the source (see the confidentiality note above).
 */
export async function createGroup(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededGroup> {
  const name = `${MARKER} group ${suffix}`;
  const id = await insert(request, "group", {
    name,
    localGroup: true,
    assignedUsers: [],
  });
  return { id, name, suffix };
}

/**
 * A project of the database that has a customer, for the order form's autocomplete.
 *
 * Read rather than created: `KundeDO` has no generated id (its number is part of KOST2 and is
 * assigned by hand), so inserting a customer means inventing an account number in someone's chart of
 * accounts. The lookup term is taken from the row at runtime instead — that keeps every customer and
 * project name out of the source, which is what the confidentiality rule is about.
 *
 * Answers null when the account sees no project with a customer (the `PM_PROJECT` right, or an empty
 * database): the caller then skips, rather than failing over missing data.
 */
export async function findProjectWithCustomer(
  request: APIRequestContext
): Promise<{ name: string; searchTerm: string } | null> {
  const res = await request.post("/rs/project/list", {
    headers: await writeHeaders(request),
    data: {},
  });
  if (!res.ok()) return null;
  const body = (await res.json()) as {
    resultSet?: { name?: string; customer?: unknown }[];
  };
  const project = (body.resultSet ?? []).find(
    (row) => row.customer != null && (row.name?.length ?? 0) >= 2
  );
  if (!project?.name) return null;
  // `EntityAutocomplete` has `minChars = 2` and asks the backend for nothing shorter, so a
  // one-letter term would look like "no project matched". The full name is the most selective term
  // available and keeps the pick unambiguous.
  return { name: project.name, searchTerm: project.name };
}

/** The logged-in account itself, as a lookup term for a user autocomplete. */
export async function ownUserSearchTerm(page: Page): Promise<string> {
  const res = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { userData } = (await res.json()) as {
    userData?: { username?: string; lastName?: string };
  };
  const name = userData?.lastName ?? userData?.username ?? "";
  if (name.length < 2) {
    throw new Error(
      "The test account has no name to search for — cannot exercise a user autocomplete."
    );
  }
  // The account matches itself, so the lookup is guaranteed to find something without naming a
  // person in the source.
  return name.slice(0, 3);
}
