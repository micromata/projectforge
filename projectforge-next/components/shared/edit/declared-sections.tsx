"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { CollapsedOnly } from "@/components/shared/collapsed-only";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { SectionDef } from "@/lib/page-def/types";
import { DeclaredFormField, fieldKey } from "./declared-form-field";

/**
 * One card of the edit page, rendered from its declaration: order, grouping, width and label.
 *
 * Which component each field gets is [DeclaredFormField]'s decision; this is only the card and the
 * three-column grid its fields sit in.
 */
export function DeclaredSection<M extends EntityMetadata>({
  section,
  metadata,
  id,
  active,
}: {
  section: SectionDef<M>;
  metadata: M;
  /** id of the entity being edited, null while adding — what a `render` body needs. */
  id: number | null;
  /**
   * Whether the tab bar above points at this section. A folded section unfolds when it becomes the
   * active one, which is what makes clicking its tab — and a `#section` in the url — do something.
   */
  active?: boolean;
}) {
  const t = useTranslations();
  const body = (
    <>
      {section.render ? (
        section.render({ id })
      ) : (
        <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
          {section.fields?.map((field) => (
            <DeclaredFormField
              key={fieldKey(field)}
              field={field}
              metadata={metadata}
            />
          ))}
        </div>
      )}
      {/* After the fields and in the same card, so it reads as part of the section — see SectionDef. */}
      {section.footer ? <section.footer id={id} /> : null}
    </>
  );

  // Through leafKeyOf, as the tab bar resolves the same key — see entityTabs.
  const title = t(leafKeyOf(section.titleKey, t.has));
  const actions = section.headerActions ? (
    <section.headerActions id={id} />
  ) : null;

  if (!section.collapsed) {
    return (
      <SectionCard>
        <SectionHeader title={title} trailing={actions} />
        {body}
      </SectionCard>
    );
  }
  const summary = section.collapsedSummary ? (
    <section.collapsedSummary id={id} />
  ) : null;
  return (
    <CollapsedSection
      title={title}
      active={active}
      trailing={actions}
      summary={summary}
    >
      {body}
    </CollapsedSection>
  );
}

/**
 * A section that starts folded — the heading is the trigger, the body is as any other card's.
 *
 * The same Collapsible a [RepeatableRow] uses, so a folded card reads like a folded row.
 */
function CollapsedSection({
  title,
  active,
  trailing,
  summary,
  children,
}: {
  title: string;
  active?: boolean;
  trailing?: ReactNode;
  /** The chips shown below the heading while folded — see SectionDef.collapsedSummary. */
  summary?: ReactNode;
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const [wasActive, setWasActive] = useState(active);
  // Becoming the active section opens it; ceasing to be it does *not* close it again — scrolling
  // past a card the user opened would fold it under their eyes.
  //
  // Adjusted while rendering rather than in an effect: this is state derived from a prop that
  // changed, so React re-renders with it before painting instead of showing the folded card for one
  // frame (react.dev, "Adjusting some state when a prop changes").
  if (active !== wasActive) {
    setWasActive(active);
    if (active) setOpen(true);
  }

  return (
    <SectionCard>
      <Collapsible open={open} onOpenChange={setOpen}>
        {/* No gap below while folded: the card is then nothing but this line. The action sits beside the
            trigger rather than inside it — it is a button (or a link) of its own, which nested in one
            would be invalid markup and would fold the card on its way. */}
        <div className={cn("flex items-center gap-3.5", open && "mb-4")}>
          {/* The heading is the whole trigger, spanning the card: the fold reacts to a click anywhere
              on that line rather than to the chevron alone. */}
          <CollapsibleTrigger className="flex flex-1 cursor-pointer text-left">
            <SectionHeader
              title={title}
              className="mb-0 flex-1"
              leading={
                <HugeiconsIcon
                  icon={ArrowDown01Icon}
                  size={14}
                  aria-hidden
                  className={cn(
                    "shrink-0 text-muted-foreground transition-transform",
                    !open && "-rotate-90"
                  )}
                />
              }
            />
          </CollapsibleTrigger>
          {trailing}
        </div>
        {/* Below the heading and only while folded ([CollapsedOnly], driven by the same `data-state` the
            order rows read): the values the fields would show, so a closed card is not a blank line. Open,
            the fields are directly below and the summary steps aside. Indented past the chevron to sit
            under the title. */}
        {summary && (
          <CollapsedOnly className="mt-1.5 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-0.5 pl-[1.375rem] text-xs text-muted-foreground">
            {summary}
          </CollapsedOnly>
        )}
        <CollapsibleContent>{children}</CollapsibleContent>
      </Collapsible>
    </SectionCard>
  );
}
