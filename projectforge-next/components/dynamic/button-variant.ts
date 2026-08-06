import type { UIColorName } from "@/lib/rs/types";

/** The variants components/ui/button.tsx offers. */
export type ButtonVariant =
  | "default"
  | "destructive"
  | "outline"
  | "secondary"
  | "ghost"
  | "link";

/**
 * Maps a server-side button style onto a shadcn button variant.
 *
 * `UIButton` describes its look as a Bootstrap `color` plus an `outline` flag (the legacy renderer
 * passed both straight to reactstrap). There is no `style` field on the wire - reading one is why
 * every dynamic button used to fall through to `outline`.
 */
export function buttonVariant(
  color?: UIColorName,
  outline?: boolean
): ButtonVariant {
  if (outline) return "outline";
  switch (color) {
    case "danger":
      return "destructive";
    case "primary":
    case "success":
    case "dark":
      return "default";
    case "secondary":
    case "info":
    case "warning":
      return "secondary";
    case "link":
      return "link";
    case "light":
      return "ghost";
    default:
      return "outline";
  }
}
