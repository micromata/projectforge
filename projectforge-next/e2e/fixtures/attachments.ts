import type { Page } from "@playwright/test";

/**
 * Removes the attachments an earlier run left on an entity, through the REST API.
 *
 * The attachment specs write to a *real* book, and the JCR keeps no history of a removed file, so
 * every one of them deletes what it uploaded. What they cannot do is delete what a run that was
 * killed mid-test uploaded — and a leftover is not inert: the column spec asserts the count the list
 * shows ("(1)"), and the duplicate spec asserts a single refusal row, so one stray file makes both
 * fail on a number that is simply one too high. That reads as a bug in the column or in the upload
 * and is neither.
 *
 * Restricted to the `pf-e2e-` prefix, which every file these specs upload carries: the book belongs
 * to a developer's local instance, and a file put there by hand is not the tests' to delete.
 */
const TEST_FILE_PREFIX = "pf-e2e-";

export async function purgeTestAttachments(
  page: Page,
  entity: string,
  id: number
): Promise<void> {
  const headers = { "X-PF-Frontend": "next" };
  const dto = await page.request.get(`/rs/${entity}/${id}`, { headers });
  if (!dto.ok()) return;
  const { attachments } = (await dto.json()) as {
    attachments?: { fileId: string; name: string }[] | null;
  };
  const leftovers = (attachments ?? []).filter((a) =>
    a.name.startsWith(TEST_FILE_PREFIX)
  );
  if (leftovers.length === 0) return;

  // multiDelete rather than a loop: every single delete rewrites the entity's JCR node and answers
  // with the whole remaining list (see lib/rs/attachments.ts).
  const status = await page.request.get("/rs/userStatus", { headers });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  await page.request.post("/rs/attachments/multiDelete", {
    headers: {
      ...headers,
      "X-PF-CSRF-Token": csrfToken,
      "Content-Type": "application/json",
    },
    data: {
      data: {
        // The backend's `category` is the entity's rest path (`AbstractPagesRest.category`).
        category: entity,
        id,
        fileIds: leftovers.map((a) => a.fileId),
        listId: "attachments",
      },
    },
  });
}
