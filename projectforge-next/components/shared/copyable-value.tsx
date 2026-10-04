"use client";

import { useState, type ReactNode } from "react";
import { secretPeek } from "@/lib/secret-peek";
import { cn } from "@/lib/utils";
import { CopyButton } from "./copy-button";
import { RevealButton } from "./reveal-button";

interface Props {
  value: string | null | undefined;
  /** Accessible name of the value, as there is no visible label tied to it. */
  label: string;
  /**
   * A secret that is passed on but shouldn't be read over one's shoulder — the password of a data
   * transfer area: hidden until the eye reveals it. Copying still gives the value itself.
   */
  masked?: boolean;
  /** With `masked`: the beginning of the secret shows through, fading out (see secretPeek). */
  peek?: boolean;
  /** Further controls after the value — a data transfer link's "renew". */
  children?: ReactNode;
  className?: string;
}

/**
 * A value to pass on rather than to edit — a subscription url, the link of a data transfer area —
 * with a button putting it on the clipboard. The button's tick says it worked, and goes again after a
 * moment (see CopyButton).
 *
 * Deliberately not a read-only input: one looks like it could be typed into and takes a focus ring
 * on a click, and then nothing can be typed. This is text on a muted ground instead — the legacy
 * ReadonlyField's look —, selectable by hand like any text, the buttons inside on the right.
 *
 * A masked value is not in the page's text while hidden, only its peek (if any); starts hidden on
 * every mount, as SecretInput does.
 */
export function CopyableValue({
  value,
  label,
  masked,
  peek,
  children,
  className,
}: Props) {
  const [revealed, setRevealed] = useState(false);
  const hidden = masked === true && !revealed;
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <div
        role="group"
        aria-label={label}
        className="flex min-h-7 min-w-0 flex-1 items-center gap-1 rounded-md bg-muted/60 py-0.5 pr-1 pl-2"
      >
        <span
          data-slot="copyable-value"
          className="min-w-0 flex-1 font-mono text-sm break-all select-text md:text-xs/relaxed"
        >
          {!value ? null : !hidden ? (
            value
          ) : peek ? (
            <span
              data-slot="secret-peek"
              aria-hidden
              className="secret-peek inline-block"
            >
              {secretPeek(value)}
            </span>
          ) : (
            <span aria-hidden className="text-muted-foreground">
              ••••••••
            </span>
          )}
        </span>
        {masked && (
          <RevealButton
            revealed={revealed}
            onRevealedChange={setRevealed}
            label={label}
            disabled={!value}
          />
        )}
        <CopyButton value={value} label={label} inline />
      </div>
      {children}
    </div>
  );
}
