/**
 * The one call of the group page (`GroupPagesRest`) that is neither a list, a read nor a write:
 * the next free LDAP gid.
 *
 * Same `PostData` envelope and `ResponseAction` answer as a write (see ./entity.ts), but nothing is
 * written — the backend only computes a number and hands the posted group back with it. So it lives
 * here rather than behind `postEntityAction`, whose result type talks about ids of written entities.
 */

import { request } from "./client";
import type { PostData, ResponseAction } from "./types";

/**
 * The next unused `gidNumber` (`LdapPosixGroupsUtils.nextFreeGidNumber`) — what the legacy form's
 * "create" button asks for.
 *
 * The whole group goes with the request because the endpoint answers the whole group back
 * (`ResponseAction(UPDATE).addVariable("data", data)`); of that answer only the number is read here,
 * so nothing the user has typed meanwhile can be overwritten by it.
 *
 * @param data The form's values, i.e. the same `Group` DTO a save would send.
 * @returns The number, or null if the backend named none (an installation without posix accounts —
 *   in which case the button isn't offered, see LdapGidField).
 */
export async function fetchNextFreeGidNumber(
  data: unknown,
  signal?: AbortSignal
): Promise<number | null> {
  const postData: PostData = { data } as PostData;
  const action = await request<ResponseAction>(
    "/rs/group/createGid",
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
  const group = action.variables?.data as { gidNumber?: number } | undefined;
  return group?.gidNumber ?? null;
}
