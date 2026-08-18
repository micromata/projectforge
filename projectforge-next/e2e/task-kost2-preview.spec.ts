import type { APIRequestContext } from "@playwright/test";
import { test, expect } from "./fixtures/auth";
import type {
  Kost2Preview,
  Kost2PreviewRequest,
  TaskNode,
} from "@/lib/rs/task";

/**
 * `POST /rs/task/kost2Preview` against the tree's own numbers — the acceptance criterion
 * projectforge-next/MIGRATION.md names for the kost2 block of step 2: *the preview equals what Wicket
 * shows*.
 *
 * Wicket's tooltip is not scraped here, and it does not have to be: its content comes from
 * `TaskTree.getKost2List` through `KostHelper.getFormattedNumberLines`, and so does the tree's
 * `kost2ListAsLines` (`TaskServicesRest.addKost2List`). Comparing the preview against the tree
 * therefore compares it against the same three calls the Wicket page makes, on every task of the tree
 * at once rather than on the one a human happened to open — and without editing a task of a database
 * that is a copy of production.
 *
 * Read-only throughout. The preview is a POST because the black/white list is form content that has no
 * business in a url (see the endpoint's own note), not because it writes anything.
 */
test.describe("kost2 preview", () => {
  test("resolves the same cost units as the tree does", async ({
    seedRequest,
  }) => {
    const nodes = await fetchTree(seedRequest);
    expect(
      nodes.length,
      "the account sees no task at all — nothing to compare"
    ).toBeGreaterThan(0);

    // The saved list of each task, so the preview is asked exactly what the tree was rendered from.
    // Every task, not only those with cost units: a task whose list resolves to nothing must answer an
    // empty preview rather than the project's whole set, and that is the more likely mistake.
    const compared = await Promise.all(
      nodes.map(async (node) => {
        const task = await fetchTask(seedRequest, node.id);
        const preview = await fetchPreview(seedRequest, {
          id: node.id,
          kost2BlackWhiteList: task.kost2BlackWhiteList,
          kost2IsBlackList: task.kost2IsBlackList === true,
        });
        return {
          id: node.id,
          tree: lines(node.kost2ListAsLines),
          preview: lines(preview.kost2ListAsLines),
        };
      })
    );

    // Reported as one object rather than asserted per task: a single differing task then names itself
    // in the failure, instead of the run stopping at the first one and hiding how many there are.
    const differing = compared.filter((row) => row.tree !== row.preview);
    expect(differing, "the preview disagrees with the tree").toEqual([]);

    // A tree whose every task resolves to nothing would pass the comparison above without having
    // compared anything — this installation has cost units, so some task must resolve some.
    const resolved = compared.filter((row) => row.preview.length > 0);
    expect(
      resolved.length,
      "no task resolved a cost unit, so the agreement above proves nothing"
    ).toBeGreaterThan(0);
  });

  test("a black list is the complement of the white list of the same entries", async ({
    seedRequest,
  }) => {
    const task = await fetchTaskWithKost2(seedRequest);
    test.skip(
      task == null,
      "no task of this database resolves a cost unit — nothing to complement"
    );
    const id = task!.id;

    // Everything the task's project offers, which is what an empty list means (`getKost2List` filters
    // nothing then). The one number to exclude is taken from that answer, so this works on any
    // database rather than on one carrying a known cost number.
    const all = numbers(await fetchPreview(seedRequest, { id }));
    expect(all.length).toBeGreaterThan(1);
    const excluded = all[0];
    // The two Kost2Art digits — the suffix form the list is matched by, and what the picker appends.
    const suffix = excluded.slice(-2);

    const white = numbers(
      await fetchPreview(seedRequest, {
        id,
        kost2BlackWhiteList: suffix,
      })
    );
    const black = numbers(
      await fetchPreview(seedRequest, {
        id,
        kost2BlackWhiteList: suffix,
        kost2IsBlackList: true,
      })
    );
    expect(white).toEqual([excluded]);
    expect(black).toEqual(all.filter((number) => number !== excluded));
    // A wild card is every unit again — the form the tree shows, and a list that must not be read as
    // a literal suffix.
    expect(
      numbers(await fetchPreview(seedRequest, { id, kost2BlackWhiteList: "*" }))
    ).toEqual(all);
  });

  test("the list is normalized, and an unknown task is empty rather than an error", async ({
    seedRequest,
  }) => {
    const task = await fetchTaskWithKost2(seedRequest);
    test.skip(task == null, "no task of this database resolves a cost unit");
    const all = numbers(await fetchPreview(seedRequest, { id: task!.id }));
    const [first, second] = [all[0].slice(-2), all[1].slice(-2)];

    // What the field sends after a user has typed in it: the server answers the list it stored, so the
    // form shows the same text a save would have produced (`TaskHelper.normalizeKost2BlackWhiteList`).
    const messy = `  ${second} ,  ${first} ; ${first}  `;
    const normalized = await fetchPreview(seedRequest, {
      id: task!.id,
      kost2BlackWhiteList: messy,
    });
    expect(normalized.kost2BlackWhiteList).toBe(
      [first, second].sort().join(",")
    );

    // The preview is asked while a task is being typed, so an id that resolves nothing is a normal
    // state and not a failure — the block then shows the em dash (see kost2-block.tsx).
    const unknown = await fetchPreview(seedRequest, { id: 0 });
    expect(unknown.projektKost ?? null).toBeNull();
    expect(unknown.kost2ListAsLines ?? null).toBeNull();
  });
});

/** The whole tree as the panel asks for it, root included where the account may see it. */
async function fetchTree(request: APIRequestContext): Promise<TaskNode[]> {
  const response = await request.get(
    "/rs/task/tree?table=true&showRootForAdmins=true&opened=true&notOpened=true&closed=true&deleted=false",
    { headers: { "X-PF-Frontend": "next" } }
  );
  expect(response.status(), "could not read the task tree").toBe(200);
  return ((await response.json()) as { nodes?: TaskNode[] }).nodes ?? [];
}

/** The task's own saved black/white list, from its page's DTO. */
async function fetchTask(
  request: APIRequestContext,
  id: number
): Promise<{
  kost2BlackWhiteList?: string | null;
  kost2IsBlackList?: boolean;
}> {
  const response = await request.get(`/rs/task/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  expect(response.status(), `could not read task ${id}`).toBe(200);
  return await response.json();
}

/**
 * One preview.
 *
 * `kost2IsBlackList` is optional here where the contract has it required: it is a primitive on the
 * request and a white list is what every case but one asks about, so defaulting it keeps the cases
 * about the list they pass.
 */
async function fetchPreview(
  request: APIRequestContext,
  body: Omit<Kost2PreviewRequest, "kost2IsBlackList"> & {
    kost2IsBlackList?: boolean;
  }
): Promise<Kost2Preview> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  const response = await request.post("/rs/task/kost2Preview", {
    headers: {
      "X-PF-Frontend": "next",
      "X-PF-CSRF-Token": csrfToken,
      "Content-Type": "application/json",
    },
    data: { kost2IsBlackList: false, ...body } satisfies Kost2PreviewRequest,
  });
  expect(response.status(), "the preview was refused").toBe(200);
  return await response.json();
}

/** The first task of the tree that resolves cost units at all, or null on a database without one. */
async function fetchTaskWithKost2(
  request: APIRequestContext
): Promise<TaskNode | null> {
  const nodes = await fetchTree(request);
  return nodes.find((node) => (node.kost2ListAsLines ?? "").length > 0) ?? null;
}

/** The lines as one comparable string — the trailing newline is not part of what is shown. */
function lines(value: string | null | undefined): string {
  return (value ?? "").trim();
}

/**
 * The cost numbers of a preview, without the titles.
 *
 * A line is `4.444.00.01: Einarbeitung …`, truncated by the backend to a tooltip width — so only the
 * number in front of the colon is a value a test may compare.
 */
function numbers(preview: Kost2Preview): string[] {
  return lines(preview.kost2ListAsLines)
    .split("\n")
    .filter((line) => line.length > 0)
    .map((line) => line.split(":")[0].trim());
}
