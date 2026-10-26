/**
 * The avatars of the users (`UserAvatarRest`). A change takes effect at once, not with the save of a form. Who
 * may see an avatar, the server decides: without the right to see it the version is null and the initials are
 * shown.
 */

import { rawRequest, RsError } from "./client";
import { uploadWithProgress } from "./upload";

const BASE = "/rs/userAvatar";

/** The url of the avatar (or of its small preview for chat and menu), versioned so the browser may cache it. */
export function userAvatarUrl(
  userId: number,
  version: number,
  preview = false
): string {
  return `${BASE}/${userId}${preview ? "/preview" : ""}?v=${version}`;
}

/** Stores the image (cut square in the image editor) as avatar of the user; resolves to its new version. */
export async function uploadUserAvatar(
  userId: number,
  file: File
): Promise<number | null> {
  const body = new FormData();
  body.append("file", file);
  const path = `${BASE}/${userId}`;
  const res = await uploadWithProgress(path, body);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, res.text || `${res.status}: ${path}`);
  }
  return (JSON.parse(res.text) as { avatarVersion: number | null })
    .avatarVersion;
}

export async function deleteUserAvatar(userId: number): Promise<void> {
  const path = `${BASE}/${userId}`;
  const res = await rawRequest(path, { method: "DELETE" });
  if (!res.ok) throw new RsError(res.status, `${res.status}: ${path}`);
}
