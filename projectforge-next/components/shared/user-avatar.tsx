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

/** The round avatar of a user, showing the initials of the name (no pictures). */
export function UserAvatar({
  name,
  className,
}: {
  name: string | null | undefined;
  className?: string;
}) {
  return (
    <div
      aria-hidden
      className={cn(
        "flex size-7 shrink-0 items-center justify-center rounded-full border-[1.5px] text-[11px] font-bold",
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
