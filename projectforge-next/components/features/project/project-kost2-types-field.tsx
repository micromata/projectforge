"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import type { ProjectValues } from "./project-schema";
import type { Kost2ArtSelection } from "./types";

/**
 * The cost 2 types of the project, as the former Wicket `ProjektEditForm` offered them: one checkbox per type,
 * "04 Name". A checked type is one the project has an active cost 2 unit of; the save
 * (`ProjectEntityRest.onAfterSaveOrUpdate`) creates or reactivates it. Unchecking an existing one sets
 * its cost 2 unit non-active — never deleted: no new time sheets can be booked on it, the ones booked keep
 * it. An existing non-active one says so and may be checked again. The types of an ended project (also one
 * set to ended in this form) are read-only, the save leaves its cost 2 units alone.
 *
 * The project standard types are coloured as in the former Wicket form: green when checked, red while one is still
 * unchecked. A type whose costs are not invoiced says so (Wicket's "(nf)"). A button checks all project
 * standard types at once; it is hidden once none is left to check.
 *
 * A custom field because `kost2Arts` is a list of the DTO only (see project-schema.ts); each box binds
 * to `kost2Arts[i].selected` in form state.
 */
export function ProjectKost2TypesField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const arts = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.kost2Arts
  ) as Kost2ArtSelection[];
  // The status as it will be saved: ending the project in this form locks the types right away.
  const ended = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.status === "ENDED"
  ) as boolean;
  const locked = readOnly || ended;
  // An ended project keeps its cost 2 units as they are (the save ignores the boxes), so show them so.
  const isChecked = (art: Kost2ArtSelection) =>
    ended ? art.active : art.selected;
  const legend = t("fibu.kost2art.kost2arten");
  // The project standard types not checked yet: missing, non-active, or unchecked in this form.
  const missingStandards = arts.filter(
    (art) => art.projektStandard && !isChecked(art)
  );

  const selectStandards = () =>
    arts.forEach((art, index) => {
      if (art.projektStandard) {
        form.setFieldValue(`kost2Arts[${index}].selected`, true);
      }
    });

  return (
    <fieldset className={cn("flex flex-col gap-1.5", className)}>
      <legend className="flex w-full items-center justify-between gap-2 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        <span>{legend}</span>
        {!locked && missingStandards.length > 0 && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="normal-case tracking-normal"
            onClick={selectStandards}
          >
            {t("fibu.projekt.edit.selectStandardKost2Arts")}
          </Button>
        )}
      </legend>
      {ended && (
        <p className="text-xs text-muted-foreground">
          {t("fibu.projekt.edit.kost2LockedEnded")}
        </p>
      )}
      <div className="grid grid-cols-1 gap-x-6 gap-y-1 sm:grid-cols-2">
        {arts.map((art, index) => {
          const label = `${String(art.id).padStart(2, "0")} ${art.name ?? ""}`;
          const checked = isChecked(art);
          return (
            <label
              key={art.id}
              data-kost2-art={art.id}
              className={cn(
                "flex items-center gap-2 text-sm",
                art.projektStandard &&
                  (checked ? "text-emerald-600" : "text-destructive")
              )}
            >
              <Checkbox
                checked={checked}
                disabled={locked}
                aria-label={label}
                onCheckedChange={(value) =>
                  form.setFieldValue(
                    `kost2Arts[${index}].selected`,
                    value === true
                  )
                }
              />
              <HintTooltip
                text={
                  art.active && !locked
                    ? t("fibu.projekt.edit.kost2DeactivateHint")
                    : art.description
                }
                plain
                openOnTap
              >
                <span>
                  <span className="font-mono">
                    {String(art.id).padStart(2, "0")}
                  </span>{" "}
                  {art.name}
                </span>
              </HintTooltip>
              {art.existsAlready && !art.active && (
                <span className="text-xs text-muted-foreground">
                  ({t("fibu.kost.status.nonactive")})
                </span>
              )}
              {art.fakturiert === false && (
                <span className="text-xs text-muted-foreground">
                  ({t("fibu.kost2art.notInvoiced")})
                </span>
              )}
            </label>
          );
        })}
      </div>
    </fieldset>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<ProjectValues, "kost2Arts" | "status">;
}
