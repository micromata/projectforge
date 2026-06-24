"use client";

import type { ComponentType } from "react";
import type { DynamicLayoutNode } from "@/lib/rs/types";
import { DynamicGroup } from "./components/dynamic-group";
import { DynamicFieldset } from "./components/dynamic-fieldset";
import { DynamicLabel } from "./components/dynamic-label";
import { DynamicInput } from "./components/dynamic-input";
import { DynamicCheckbox } from "./components/dynamic-checkbox";
import { DynamicSelect } from "./components/dynamic-select";
import { DynamicTextarea } from "./components/dynamic-textarea";
import { DynamicButton } from "./components/dynamic-button";
import { DynamicReadonlyField } from "./components/dynamic-readonly-field";
import { DynamicAlert } from "./components/dynamic-alert";
import { DynamicTable } from "./components/dynamic-table";
import { DynamicBadge } from "./components/dynamic-badge";
import { DynamicSpacer } from "./components/dynamic-spacer";
import { DynamicFallback } from "./components/dynamic-fallback";

export interface DynamicComponentProps {
  node: DynamicLayoutNode;
}

const COMPONENT_MAP: Record<string, ComponentType<DynamicComponentProps>> = {
  ROW: DynamicGroup,
  COL: DynamicGroup,
  GROUP: DynamicGroup,
  FRAGMENT: DynamicGroup,
  INLINE_GROUP: DynamicGroup,
  FIELDSET: DynamicFieldset,
  LABEL: DynamicLabel,
  INPUT: DynamicInput,
  CHECKBOX: DynamicCheckbox,
  SELECT: DynamicSelect,
  CREATABLE_SELECT: DynamicSelect,
  TEXTAREA: DynamicTextarea,
  BUTTON: DynamicButton,
  READONLY_FIELD: DynamicReadonlyField,
  ALERT: DynamicAlert,
  TABLE: DynamicTable,
  TABLE_LIST_PAGE: DynamicTable,
  AG_GRID: DynamicTable,
  AG_GRID_LIST_PAGE: DynamicTable,
  BADGE: DynamicBadge,
  BADGE_LIST: DynamicBadge,
  SPACER: DynamicSpacer,
};

export function DynamicRenderer({ content }: { content?: DynamicLayoutNode[] }) {
  if (!content || content.length === 0) return null;

  return (
    <>
      {content.map((node) => {
        const Component = COMPONENT_MAP[node.type] ?? DynamicFallback;
        return <Component key={node.key} node={node} />;
      })}
    </>
  );
}
