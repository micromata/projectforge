"use client";

import { useState } from "react";
import { isInactiveRef, type InactiveFlags } from "@/lib/page-def/reference";
import { userAvatarUrl } from "@/lib/rs/user-avatar";
import { cn } from "@/lib/utils";

/** Two initials of a name like "Fink, Laura" or "Laura Fink", the content of an avatar. */
export function initials(name: string | null | undefined): string {
  if (!name) return "?";
  return name
    .split(/[\s,]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

/**
 * The round avatar of a user: the picture if the user has one (`userId` and `avatarVersion`, see
 * lib/rs/user-avatar.ts), otherwise — or if it can't be loaded — the initials of the name. A deleted or
 * deactivated user (`flags`, see isInactiveRef) reads dimmed and grey, next to the struck-through name.
 * `full` loads the picture in full size instead of the small preview, for large avatars.
 */
export function UserAvatar({
  name,
  flags,
  userId,
  avatarVersion,
  full = false,
  className,
}: {
  name: string | null | undefined;
  flags?: InactiveFlags | null;
  userId?: number | null;
  avatarVersion?: number | null;
  full?: boolean;
  className?: string;
}) {
  const src =
    userId != null && avatarVersion != null
      ? userAvatarUrl(userId, avatarVersion, !full)
      : null;
  // Remembered per url, so a new version is tried again.
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const classes = cn(
    "flex size-7 shrink-0 items-center justify-center rounded-full border-[1.5px] text-[11px] font-bold",
    isInactiveRef(flags) && "opacity-50 grayscale",
    className
  );
  if (src && src !== failedSrc) {
    return (
      // A plain img: served by the backend, nothing next/image could optimize under the static export.
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={src}
        alt=""
        aria-hidden
        loading="lazy"
        onError={() => setFailedSrc(src)}
        className={cn(classes, "object-cover")}
        style={{ borderColor: "var(--status-available-border)" }}
      />
    );
  }
  return (
    <div
      aria-hidden
      className={classes}
      style={{
        background: "var(--status-available-bg)",
        borderColor: "var(--status-available-border)",
        color: "var(--primary)",
      }}
    >
      {initials(name)}
    </div>
  );
}
