import { cn } from "@/lib/utils";

export interface StatusBadgeProps {
  /** True when the book is currently lent out. */
  lendOut: boolean;
  /** Localized label. Caller provides the translated text. */
  label: string;
  className?: string;
  variant?: "pill" | "compact";
}

export function StatusBadge({
  lendOut,
  label,
  className,
  variant = "compact",
}: StatusBadgeProps) {
  const compact = variant === "compact";
  return (
    <span
      className={cn(
        "inline-flex items-center whitespace-nowrap rounded-full border font-semibold",
        compact ? "px-2 py-0 text-[10px]" : "gap-1.5 px-2.5 py-0.5 text-xs",
        className
      )}
      style={
        lendOut
          ? {
              background: "var(--status-loaned-bg)",
              color: "var(--status-loaned)",
              borderColor: "var(--status-loaned-border)",
            }
          : {
              background: "var(--status-available-bg)",
              color: "var(--status-available)",
              borderColor: "var(--status-available-border)",
            }
      }
    >
      <span
        aria-hidden
        className={cn(
          "rounded-full bg-current",
          compact ? "mr-1 size-1" : "size-1.5"
        )}
      />
      {label}
    </span>
  );
}
