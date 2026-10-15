"use client";

import type { ComponentType } from "react";
import type { DynamicLayoutNode } from "@/lib/rs/types";
import { DynamicGroup } from "./components/dynamic-group";
import { DynamicFieldset } from "./components/dynamic-fieldset";
import { DynamicLabel } from "./components/dynamic-label";
import { DynamicInputResolver } from "./components/input/dynamic-input-resolver";
import { DynamicCheckbox } from "./components/dynamic-checkbox";
import { DynamicSelect } from "./components/select/dynamic-select";
import { DynamicRadioButton } from "./components/dynamic-radiobutton";
import { DynamicList } from "./components/dynamic-list";
import { DynamicTextarea } from "./components/dynamic-textarea";
import { DynamicButton } from "./components/dynamic-button";
import { DynamicReadonlyField } from "./components/dynamic-readonly-field";
import { DynamicAlert } from "./components/dynamic-alert";
import { DynamicGrid } from "./components/grid/dynamic-grid";
import { DynamicBadge } from "./components/dynamic-badge";
import { DynamicSpacer } from "./components/dynamic-spacer";
import { DynamicFallback } from "./components/dynamic-fallback";
import { DynamicCustomized } from "./customized/dynamic-customized";

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
  INPUT: DynamicInputResolver,
  CHECKBOX: DynamicCheckbox,
  SELECT: DynamicSelect,
  CREATABLE_SELECT: DynamicSelect,
  RADIOBUTTON: DynamicRadioButton,
  LIST: DynamicList,
  TEXTAREA: DynamicTextarea,
  BUTTON: DynamicButton,
  READONLY_FIELD: DynamicReadonlyField,
  ALERT: DynamicAlert,
  TABLE: DynamicGrid,
  TABLE_LIST_PAGE: DynamicGrid,
  AG_GRID: DynamicGrid,
  AG_GRID_LIST_PAGE: DynamicGrid,
  BADGE: DynamicBadge,
  BADGE_LIST: DynamicBadge,
  SPACER: DynamicSpacer,
  CUSTOMIZED: DynamicCustomized,
};

export function DynamicRenderer({
  content,
}: {
  content?: DynamicLayoutNode[];
}) {
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
