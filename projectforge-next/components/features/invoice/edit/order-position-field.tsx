"use client";

import { useTranslations } from "next-intl";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { OrderPositionLink } from "./order-position-link";
import type { InvoicePositionValues } from "../invoice-schema";

/** One hit of `order/positionAutosearch` (`OrderEntityRest.OrderPositionHit`). */
interface OrderPositionHit {
  id: number;
  auftragId?: number | null;
  auftragNummer?: number | null;
  number?: number | null;
  displayName: string;
}

/**
 * Which order position this invoice position bills — Wicket's `AuftragsPositionFormComponent` with the
 * GOTO icon beside it, as two characters minimum and a contains-match like there.
 *
 * Not the shared [EntityAutocompleteField]: that binds a field holding `{id, displayName}`, and the value
 * here is richer (see the schema's `auftragsPosition`) — the order's id and number come with the hit
 * because the link beside the field and the collapsed row header need them, and nothing else on the page
 * knows them. So the picker is bound directly, translating between the two shapes, with the same manual
 * blur that field does: the control is a popover, so nothing else would ever mark it touched.
 *
 * `order/positionAutosearch` and not the generic `autosearch`, which searches whole orders — see the
 * endpoint for why the positions need one of their own.
 */
export function OrderPositionField({
  name,
  className,
}: {
  /** Full path of the value, e.g. `positionen[2].auftragsPosition`. */
  name: string;
  className?: string;
}) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  // `._` is the bare key of a namespace that also has children (`fibu.auftrag.position.art`).
  const label = `${t("fibu.auftrag._")} ${t("fibu.auftrag.position._")}`;

  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const value = field.state
          .value as InvoicePositionValues["auftragsPosition"];
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        return (
          <FieldShell
            label={label}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div className="flex min-w-0 items-center gap-2">
              <EntityAutocomplete
                id={ids.controlId}
                aria-label={label}
                url="order/positionAutosearch?search=:search"
                // The picker speaks `{id, displayName}`; a stored reference has no displayName of its
                // own, so the number the link shows stands in for it until one is picked.
                value={
                  value?.id == null
                    ? null
                    : {
                        id: value.id,
                        displayName:
                          value.displayName ??
                          `${value.auftragNummer ?? ""}.${value.number ?? ""}`,
                      }
                }
                minChars={2}
                onChange={(picked) => {
                  const hit = picked as OrderPositionHit | null;
                  field.handleChange(
                    hit == null
                      ? null
                      : {
                          id: hit.id,
                          auftragId: hit.auftragId ?? null,
                          auftragNummer: hit.auftragNummer ?? null,
                          number: hit.number ?? null,
                          displayName: hit.displayName,
                        }
                  );
                  field.handleBlur();
                }}
                className="min-w-0 flex-1"
              />
              <OrderPositionLink order={value} className="text-xs" />
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
