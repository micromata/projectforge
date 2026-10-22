"use client";

import {
  useRef,
  useState,
  type DragEvent,
  type FocusEvent,
  type KeyboardEvent,
} from "react";
import { useTranslations } from "next-intl";
import { useChipSelection } from "@/hooks/use-chip-selection";
import { useFocusedClipboard } from "@/hooks/use-focused-clipboard";
import { carriesRefs, readRefs, writeRefs } from "@/lib/entity-ref-transfer";
import { toast } from "@/lib/toast";
import type { EntityRef } from "./entity-autocomplete";
import type { EntityChipProps } from "./entity-chip";

/**
 * Moves picked entities between the pickers of [EntityMultiAutocomplete] of the same kind (`refType`):
 * chips are selected by click (see [useChipSelection]), then dragged onto another picker, or copied
 * with Ctrl/Cmd+C and pasted into one with Ctrl/Cmd+V — on another page or in another tab too.
 * Dropping and pasting add; the source keeps its chips. What is there already is skipped silently, as a
 * second pick of the same entry is.
 *
 * Returns the props of the box (focusable, so the keys are its own) and of each chip.
 */
export function useChipTransfer({
  refType,
  value,
  chips,
  onChange,
}: {
  refType: string;
  value: EntityRef[];
  /** The value as shown — sorted or not. */
  chips: EntityRef[];
  onChange: (value: EntityRef[]) => void;
}) {
  const t = useTranslations();
  const boxRef = useRef<HTMLDivElement>(null);
  const selection = useChipSelection(chips.map((chip) => chip.id));
  const [dropTarget, setDropTarget] = useState(false);
  // Set while a drag starts here: dropping the chips back onto their own box would add nothing.
  const draggingFromHere = useRef(false);

  const addRefs = (refs: EntityRef[]) => {
    const known = new Set(value.map((picked) => picked.id));
    const added: EntityRef[] = [];
    for (const ref of refs) {
      if (known.has(ref.id)) continue;
      known.add(ref.id);
      added.push(ref);
    }
    if (added.length > 0) onChange([...value, ...added]);
  };
  const selectedOrAll = () =>
    selection.selectedIds.length > 0
      ? chips.filter((chip) => selection.isSelected(chip.id))
      : chips;

  useFocusedClipboard(boxRef, {
    onCopy: (data) => writeRefs(data, refType, selectedOrAll()),
    onPaste: (data) => {
      const refs = readRefs(data, refType);
      if (refs.length === 0) toast(t("entityRefs.paste.foreign"));
      else addRefs(refs);
    },
  });

  const takesDrag = (event: DragEvent) =>
    !draggingFromHere.current && carriesRefs(event.dataTransfer, refType);

  const boxProps = {
    ref: boxRef,
    tabIndex: -1,
    role: "listbox",
    "aria-multiselectable": true,
    // From anywhere in the box — the box itself, the trigger, a removing button: none of them is a text
    // field these keys would mean something else in.
    onKeyDown: (event: KeyboardEvent) => {
      if (event.key === "Escape" && selection.selectedIds.length > 0) {
        event.stopPropagation();
        selection.clear();
      } else if (event.key === "Delete" || event.key === "Backspace") {
        const gone = new Set(selection.selectedIds);
        if (gone.size > 0)
          onChange(value.filter((picked) => !gone.has(picked.id)));
      } else if (event.key === "a" && (event.ctrlKey || event.metaKey)) {
        event.preventDefault();
        selection.selectAll();
      }
    },
    onBlur: (event: FocusEvent) => {
      if (!event.currentTarget.contains(event.relatedTarget)) selection.clear();
    },
    onDragOver: (event: DragEvent) => {
      if (!takesDrag(event)) return;
      event.preventDefault();
      event.dataTransfer.dropEffect = "copy";
      setDropTarget(true);
    },
    onDragLeave: (event: DragEvent) => {
      if (!event.currentTarget.contains(event.relatedTarget as Node | null))
        setDropTarget(false);
    },
    onDrop: (event: DragEvent) => {
      setDropTarget(false);
      if (!takesDrag(event)) return;
      event.preventDefault();
      addRefs(readRefs(event.dataTransfer, refType));
    },
  };

  const chipProps = (
    entry: EntityRef,
    removeLabel: string
  ): EntityChipProps => ({
    entry,
    removeLabel,
    selected: selection.isSelected(entry.id),
    onSelect: (modifiers) => selection.select(entry.id, modifiers),
    onRemove: () => onChange(value.filter((e) => e.id !== entry.id)),
    onDragStart: (event) => {
      // A selected chip takes the whole selection along, an unselected one only itself.
      const dragged = selection.isSelected(entry.id)
        ? chips.filter((chip) => selection.isSelected(chip.id))
        : [entry];
      writeRefs(event.dataTransfer, refType, dragged);
      event.dataTransfer.effectAllowed = "copy";
      draggingFromHere.current = true;
    },
    onDragEnd: () => {
      draggingFromHere.current = false;
    },
  });

  return { boxProps, chipProps, dropTarget };
}
