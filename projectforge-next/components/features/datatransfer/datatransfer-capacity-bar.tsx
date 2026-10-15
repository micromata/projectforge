import { FillLevelBar } from "@/components/shared/fill-level-bar";
import type { DataTransferCapacity } from "@/lib/rs/datatransfer";

/**
 * How full an area is (see FillLevelBar). Computed from the bytes, not the backend's rounded
 * percentage: 11 MB of 20 GB is "0%", but a non-empty area keeps a visible sliver. Nothing is rendered
 * without a known capacity.
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
  return (
    <FillLevelBar
      used={capacity?.used ?? 0}
      max={capacity?.capacity ?? 0}
      label={label}
      className={className}
    />
  );
}
