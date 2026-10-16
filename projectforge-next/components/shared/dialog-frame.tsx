import type { ReactNode } from "react";

/**
 * Stands in for a SectionCard where a form is rendered in a dialog (the direct call, the text message): the dialog
 * draws the frame already, a card inside it would be a second one.
 */
export function DialogFrame({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  return <div className={className}>{children}</div>;
}
