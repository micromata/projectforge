"use client";

import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";

/**
 * Hours as the legacy list showed them: two decimals, and nothing at all for none — a week is read by
 * the days that *are* planned.
 */
export function Hours({
  value,
  className,
}: {
  value: number | null | undefined;
  className?: string;
}) {
  const format = useFormatContext();
  if (!value) return null;
  return <span className={className}>{formatNumber(value, format, 2)}</span>;
}
