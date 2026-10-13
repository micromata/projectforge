"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  Comment01Icon,
  InformationCircleIcon,
  Redo02Icon,
  SearchReplaceIcon,
  Undo02Icon,
} from "@hugeicons/core-free-icons";
import type { EditorView } from "@uiw/react-codemirror";
import { redo, toggleComment, undo } from "@codemirror/commands";
import { openSearchPanel } from "@codemirror/search";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { CodeEditorShortcutList } from "./code-editor-shortcut-list";
import {
  ShortcutKeys,
  shortcutOf,
  type CodeEditorCommand,
} from "./code-editor-shortcuts";

/**
 * The commands of the editor as buttons above it, each naming its keyboard shortcut in the tooltip, and
 * an info button with all shortcuts — what a user doesn't know is there is not there for them.
 *
 * A read-only editor keeps search only.
 */
export function CodeEditorToolbar({
  view,
  readOnly,
}: {
  /**
   * The editor's view, read when a button is pressed: `@uiw/react-codemirror` creates it anew where it
   * has to (React's StrictMode remounts), so a view kept from an earlier render may be a destroyed one.
   */
  view: () => EditorView | undefined;
  readOnly?: boolean;
}) {
  const t = useTranslations();
  return (
    <div
      role="toolbar"
      className="flex items-center gap-0.5 border-b bg-muted/40 px-1 py-0.5"
    >
      {!readOnly && (
        <>
          <CommandButton
            view={view}
            command="undo"
            label={t("codeEditor.undo")}
            icon={Undo02Icon}
            run={undo}
          />
          <CommandButton
            view={view}
            command="redo"
            label={t("codeEditor.redo")}
            icon={Redo02Icon}
            run={redo}
          />
          <Separator orientation="vertical" className="mx-0.5 my-1" />
        </>
      )}
      <CommandButton
        view={view}
        command="search"
        label={t("codeEditor.search")}
        icon={SearchReplaceIcon}
        run={openSearchPanel}
      />
      {!readOnly && (
        <CommandButton
          view={view}
          command="toggleComment"
          label={t("codeEditor.toggleComment")}
          icon={Comment01Icon}
          run={toggleComment}
        />
      )}
      <Popover>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            className="ml-auto text-muted-foreground"
            aria-label={t("codeEditor.shortcuts")}
          >
            <HugeiconsIcon icon={InformationCircleIcon} strokeWidth={2} />
          </Button>
        </PopoverTrigger>
        <PopoverContent align="end" className="w-auto">
          <CodeEditorShortcutList />
        </PopoverContent>
      </Popover>
    </div>
  );
}

function CommandButton({
  view,
  command,
  label,
  icon,
  run,
}: {
  view: () => EditorView | undefined;
  command: CodeEditorCommand;
  label: string;
  icon: IconSvgElement;
  run: (view: EditorView) => boolean;
}) {
  return (
    <HintTooltip
      title={label}
      content={<ShortcutKeys shortcut={shortcutOf(command)} />}
    >
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label={label}
        // The editor keeps its focus and selection: a command acts on where the cursor is.
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => {
          const current = view();
          if (!current) return;
          current.focus();
          run(current);
        }}
      >
        <HugeiconsIcon icon={icon} strokeWidth={2} />
      </Button>
    </HintTooltip>
  );
}
