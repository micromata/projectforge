"use client";

/**
 * A tiny bar chart for a table cell: one bar per value, scaled to the row's own maximum, so the shape (steady,
 * spike, gone quiet) reads at a glance. Plain SVG: a recharts chart per row of a long list would be far too heavy
 * for so little. Wrap it in a `HintTooltip` for the figures behind it.
 */
export function Sparkline({
  values,
  ariaLabel,
  width = 112,
  height = 20,
}: {
  values: number[];
  ariaLabel?: string;
  width?: number;
  height?: number;
}) {
  const max = Math.max(1, ...values);
  const barWidth = values.length > 0 ? width / values.length : width;
  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      className="text-primary"
      role="img"
      aria-label={ariaLabel}
    >
      <line
        x1={0}
        x2={width}
        y1={height - 0.5}
        y2={height - 0.5}
        className="stroke-border"
        strokeWidth={1}
      />
      {values.map((value, index) => {
        if (value <= 0) return null;
        // At least 2px, so a single occurrence stays visible beside a spike.
        const barHeight = Math.max(2, (value / max) * height);
        return (
          <rect
            key={index}
            x={index * barWidth + 0.5}
            y={height - barHeight}
            width={Math.max(1, barWidth - 1)}
            height={barHeight}
            fill="currentColor"
          />
        );
      })}
    </svg>
  );
}
