/**
 * The one call of the group page (`GroupEntityRest`) that is neither a list, a read nor a write:
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

/** One member of a group, as [fetchGroupMembers] answers it. */
export interface GroupMember {
  id: number;
  displayName: string;
  deactivated: boolean;
}

/** The answer of `GET /rs/group/members` (`GroupEntityRest.members`). */
export interface GroupMembers {
  /** False if the logged-in user may not see the group (no member, no admin) — or it does not exist. */
  visible: boolean;
  /** Sorted by name; empty while not [visible]. */
  members: GroupMember[];
}

/** The members of a group, for the tooltip of its pill (see GroupMembersHint). */
export function fetchGroupMembers(
  id: number,
  signal?: AbortSignal
): Promise<GroupMembers> {
  return request<GroupMembers>(
    `/rs/group/members?id=${encodeURIComponent(id)}`,
    { method: "GET" },
    signal
  );
}
