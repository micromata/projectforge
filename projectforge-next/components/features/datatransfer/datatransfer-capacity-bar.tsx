import { Progress } from "@/components/ui/progress";
import type { DataTransferCapacity } from "@/lib/rs/datatransfer";
import { cn } from "@/lib/utils";

/**
 * How full an area is: green below 80%, orange below 90%, red from 90% on. Computed from the bytes,
 * not the backend's rounded percentage: 11 MB of 20 GB is "0%", but a non-empty area keeps a visible
 * sliver. Nothing is rendered without a known capacity.
 */
export function DataTransferCapacityBar({
  capacity,
  label,
  className,
}: {
  capacity?: DataTransferCapacity | null;
  label: string;
  className?: string;
}) {
  const used = capacity?.used ?? 0;
  const total = capacity?.capacity ?? 0;
  if (total <= 0) return null;
  const percent = Math.min(100, (100 * used) / total);
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
