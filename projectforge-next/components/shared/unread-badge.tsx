import { cn } from "@/lib/utils";

/** The red counter of unread messages (as the menu counters); nothing for none. */
export function UnreadBadge({
  count,
  className,
}: {
  count: number | null | undefined;
  className?: string;
}) {
  if (!count) return null;
  return (
    <span
      className={cn(
        "inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-destructive px-1 text-xs font-semibold text-white",
        className
      )}
    >
      {count > 99 ? "99+" : count}
    </span>
  );
}
