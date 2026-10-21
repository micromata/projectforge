"use client";

import { useState } from "react";
import type { KeyboardEvent } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Popover, PopoverAnchor } from "@/components/ui/popover";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import {
  useEntityEditForm,
  useFieldMetadata,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { VISITORBOOK_METADATA } from "@/lib/metadata/visitorbook.generated";
import type { ExistingVisitor } from "@/lib/rs/visitorbook";
import { cn } from "@/lib/utils";
import { ExistingVisitorsList } from "./existing-visitors-list";
import { useExistingVisitors } from "./use-existing-visitors";

type NameProperty = "firstname" | "lastname";

/**
 * The first or the last name of the visitor. While a visitor is being added, it offers the stored visitors whose
 * names start with what is typed into both fields: picking one opens that visitor with the dialog of a new visit,
 * so the visit is added to it rather than to a second visitor of the same name. Anything else typed is just the
 * name of a new visitor.
 */
function VisitorbookNameField({
  name,
  className,
}: {
  name: NameProperty;
  className?: string;
}) {
  const router = useRouter();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const label = useFieldLabels(VISITORBOOK_METADATA)(name);
  const { required, maxLength } = useFieldMetadata(name);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);

  const { existing, isNew } = useExistingVisitors(open);
  const listOpen = isNew && open && existing.length > 0;

  const pick = (visitor: ExistingVisitor) => {
    setOpen(false);
    // The visits section opens the dialog of a new visit for it (see VisitorbookVisitsSection).
    router.push(`/visitorbook/${visitor.id}?addVisit=1`);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (!listOpen) return;
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((i) => (i + 1) % existing.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((i) => (i <= 0 ? existing.length - 1 : i - 1));
    } else if (event.key === "Enter" && activeIndex >= 0) {
      event.preventDefault();
      pick(existing[activeIndex]);
    } else if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
      setActiveIndex(-1);
    }
  };

  return (
    <form.Field name={name}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        return (
          <FieldShell
            name={name}
            label={label}
            required={required}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            {/* As in SuggestInput: `open` follows the focus, the list mounts once there are hits. */}
            <Popover open={open} onOpenChange={setOpen}>
              <PopoverAnchor asChild>
                <Input
                  id={ids.controlId}
                  value={(field.state.value as string | null) ?? ""}
                  required={required}
                  maxLength={maxLength}
                  autoComplete="off"
                  className={cn(invalid && "border-destructive")}
                  role="combobox"
                  aria-expanded={listOpen}
                  aria-autocomplete="list"
                  onChange={(e) => {
                    field.handleChange(e.target.value);
                    setOpen(true);
                    setActiveIndex(-1);
                  }}
                  onKeyDown={onKeyDown}
                  onFocus={() => setOpen(true)}
                  onClick={() => setOpen(true)}
                  onBlur={() => {
                    setOpen(false);
                    setActiveIndex(-1);
                    field.handleBlur();
                  }}
                />
              </PopoverAnchor>
              {listOpen && (
                <ExistingVisitorsList
                  visitors={existing}
                  activeIndex={activeIndex}
                  onActivate={setActiveIndex}
                  onPick={pick}
                />
              )}
            </Popover>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

export function VisitorbookFirstnameField({
  className,
}: {
  className?: string;
}) {
  return <VisitorbookNameField name="firstname" className={className} />;
}

export function VisitorbookLastnameField({
  className,
}: {
  className?: string;
}) {
  return <VisitorbookNameField name="lastname" className={className} />;
}
