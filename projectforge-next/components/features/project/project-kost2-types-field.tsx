"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
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
 * The cost 2 types of the project, as Wicket's `ProjektEditForm` offers them: one checkbox per type,
 * "04 Name". A type the project already has a cost 2 unit for is checked and cannot be unchecked —
 * cost 2 units are never removed here. A newly checked one is created after the save
 * (`ProjectEntityRest.onAfterSaveOrUpdate`).
 *
 * The project standard types are coloured as in Wicket: green when the project has them, red while it
 * is still missing one. A type whose costs are not invoiced says so (Wicket's "(nf)").
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
  const legend = t("fibu.kost2art.kost2arten");

  return (
    <fieldset className={cn("flex flex-col gap-1.5", className)}>
      <legend className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {legend}
      </legend>
      <div className="grid grid-cols-1 gap-x-6 gap-y-1 sm:grid-cols-2">
        {arts.map((art, index) => {
          const label = `${String(art.id).padStart(2, "0")} ${art.name ?? ""}`;
          return (
            <label
              key={art.id}
              data-kost2-art={art.id}
              className={cn(
                "flex items-center gap-2 text-sm",
                art.projektStandard &&
                  (art.existsAlready ? "text-emerald-600" : "text-destructive")
              )}
            >
              <Checkbox
                checked={art.existsAlready || art.selected}
                disabled={readOnly || art.existsAlready}
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
                  art.existsAlready
                    ? t("fibu.projekt.edit.kost2DoesAlreadyExists")
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
  values: Pick<ProjectValues, "kost2Arts">;
}
