"use client";

import { useState, type ReactNode } from "react";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group";
import { secretPeek } from "@/lib/secret-peek";
import { cn } from "@/lib/utils";
import { CopyButton } from "./copy-button";
import { RevealButton } from "./reveal-button";

export interface SecretInputProps {
  value: string | null | undefined;
  onChange: (value: string) => void;
  onBlur?: () => void;
  id?: string;
  /** Accessible name, where no label is tied to the box by `id`. */
  "aria-label"?: string;
  /** Names the secret in the buttons' accessible names ("Show: password"). */
  label: string;
  invalid?: boolean;
  disabled?: boolean;
  maxLength?: number;
  /**
   * While hidden, the beginning of the secret shows through, fading out — enough to tell two tokens
   * apart at a glance without the whole being readable (the legacy ReadonlyField's `coverUp`).
   */
  peek?: boolean;
  /** Further controls after the box — a "renew". */
  children?: ReactNode;
  className?: string;
}

/**
 * An editable secret — a password, an access token — shown as dots until the eye reveals it, with a
 * button putting it on the clipboard either way; both inside the box. The form field around it is
 * SecretField; a secret that is only passed on is a CopyableValue.
 *
 * With `peek`, the hidden box shows the first few characters fading out (see secretPeek) instead of
 * dots only, as the legacy app did; the box below is still a password box, only its own text is made
 * invisible. Focused, the peek gives way to the whole box, dots or clear, as it is being edited.
 *
 * Starts hidden on every mount, so a secret revealed once isn't left readable on the screen the next
 * time the page is opened. `autoComplete="off"`: a password box is what browsers fill the user's own
 * login into, which must never end up as an area's password.
 */
export function SecretInput({
  value,
  onChange,
  onBlur,
  id,
  "aria-label": ariaLabel,
  label,
  invalid,
  disabled,
  maxLength,
  peek,
  children,
  className,
}: SecretInputProps) {
  const [revealed, setRevealed] = useState(false);
  // While typed into, the box shows every character — as dots or in clear — not just the peek.
  const [focused, setFocused] = useState(false);
  const peeking = peek === true && !revealed && !focused && !!value;
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <InputGroup data-disabled={disabled || undefined}>
        <InputGroupInput
          id={id}
          type={revealed ? "text" : "password"}
          className={cn(
            "font-mono",
            peeking && "text-transparent caret-foreground"
          )}
          value={value ?? ""}
          disabled={disabled}
          maxLength={maxLength}
          autoComplete="off"
          spellCheck={false}
          aria-label={ariaLabel}
          aria-invalid={invalid || undefined}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setFocused(true)}
          onBlur={() => {
            setFocused(false);
            onBlur?.();
          }}
        />
        {peeking && (
          <span
            data-slot="secret-peek"
            aria-hidden
            className="secret-peek pointer-events-none absolute inset-y-0 left-0 flex items-center whitespace-pre px-2 font-mono text-sm md:text-xs/relaxed"
          >
            {secretPeek(value)}
          </span>
        )}
        <InputGroupAddon align="inline-end">
          <RevealButton
            revealed={revealed}
            onRevealedChange={setRevealed}
            label={label}
            disabled={!value}
          />
          <CopyButton value={value} label={label} inline />
        </InputGroupAddon>
      </InputGroup>
      {children}
    </div>
  );
}
