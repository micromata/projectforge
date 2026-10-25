"use client";

import type { ReactNode } from "react";
import type { Editor } from "@tiptap/react";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Toggle } from "@/components/ui/toggle";

/** The buttons of [RichTextToolbar]: a toggle and a menu, both icon-only with their label as tooltip. */

/** Asks for the address of the link at the selection; an empty one (or just `https://`) removes the link. */
export function promptLink(editor: Editor | null, prompt: string) {
  if (!editor) return;
  const previous = editor.getAttributes("link").href as string | undefined;
  const url = window.prompt(prompt, previous ?? "https://")?.trim();
  if (url === undefined) return;
  const chain = editor.chain().focus().extendMarkRange("link");
  if (url === "" || url === "https://") {
    chain.unsetLink().run();
  } else {
    chain.setLink({ href: url }).run();
  }
}

export function ToolbarToggle({
  label,
  hint,
  pressed,
  onPressedChange,
  children,
}: {
  label: string;
  /** The tooltip, if it says more than the label. */
  hint?: string;
  pressed: boolean | undefined;
  onPressedChange: () => void;
  children: ReactNode;
}) {
  return (
    <HintTooltip title={hint ?? label}>
      <Toggle
        size="sm"
        aria-label={label}
        pressed={pressed ?? false}
        onPressedChange={onPressedChange}
        // Keeps the editor's selection: a click on the toolbar must not blur it first.
        onMouseDown={(e) => e.preventDefault()}
      >
        {children}
      </Toggle>
    </HintTooltip>
  );
}

export function ToolbarMenu({
  label,
  icon,
  pressed,
  children,
}: {
  label: string;
  icon: IconSvgElement;
  pressed?: boolean;
  children: ReactNode;
}) {
  return (
    <DropdownMenu>
      <HintTooltip title={label}>
        <DropdownMenuTrigger asChild>
          <Toggle size="sm" aria-label={label} pressed={pressed ?? false}>
            <HugeiconsIcon icon={icon} />
          </Toggle>
        </DropdownMenuTrigger>
      </HintTooltip>
      <DropdownMenuContent align="start">{children}</DropdownMenuContent>
    </DropdownMenu>
  );
}
