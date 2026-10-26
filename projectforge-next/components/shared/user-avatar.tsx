import { isInactiveRef, type InactiveFlags } from "@/lib/page-def/reference";
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
 * The round avatar of a user, showing the initials of the name (no pictures). A deleted or deactivated user
 * (`flags`, see isInactiveRef) reads dimmed and grey, next to the struck-through name.
 */
export function UserAvatar({
  name,
  flags,
  className,
}: {
  name: string | null | undefined;
  flags?: InactiveFlags | null;
  className?: string;
}) {
  return (
    <div
      aria-hidden
      className={cn(
        "flex size-7 shrink-0 items-center justify-center rounded-full border-[1.5px] text-[11px] font-bold",
        isInactiveRef(flags) && "opacity-50 grayscale",
        className
      )}
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
