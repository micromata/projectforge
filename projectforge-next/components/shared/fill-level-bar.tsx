import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

/**
 * How full something is (a disk, the memory, a connection pool): green below 80%, orange below 90%, red
 * from 90% on. A non-empty value keeps a visible sliver, even if it is rounded to 0%. Nothing is
 * rendered without a known maximum.
 */
export function FillLevelBar({
  used,
  max,
  label,
  className,
}: {
  used: number;
  max: number;
  label: string;
  className?: string;
}) {
  if (max <= 0) return null;
  const percent = fillLevelPercent(used, max);
  return (
    <Progress
      value={used > 0 ? Math.max(1, percent) : 0}
      aria-label={label}
      className={cn(
        "h-2",
        percent >= 90
          ? "[&_[data-slot=progress-indicator]]:bg-red-600"
          : percent >= 80
            ? "[&_[data-slot=progress-indicator]]:bg-orange-500"
            : "[&_[data-slot=progress-indicator]]:bg-green-600",
        className
      )}
    />
  );
}

/** The fill level in percent, 0..100. */
export function fillLevelPercent(used: number, max: number): number {
  if (max <= 0) return 0;
  return Math.max(0, Math.min(100, (100 * used) / max));
}
