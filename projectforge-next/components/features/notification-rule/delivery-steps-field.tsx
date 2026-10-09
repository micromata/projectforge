"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { DeliveryStepRow } from "./delivery-step-row";
import type { DeliveryStep } from "./types";

/**
 * The escalation cascade (NotificationDelivery): the steps in their order, e.g. in the app at once and
 * a mail after two days if not confirmed by then. Bound as one list, so a server error about the
 * cascade as a whole (no step, a negative delay) shows here.
 */
export function DeliveryStepsField({ className }: { className?: string }) {
  const t = useTranslations("notification");
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const label = t("deliverySteps");
  return (
    <form.Field name={"deliverySteps" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const steps = (field.state.value ?? []) as DeliveryStep[];
        const set = (next: DeliveryStep[]) => field.handleChange(next);
        return (
          <FieldShell
            name="deliverySteps"
            label={label}
            hint={t("delivery.info")}
            invalid={!meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div
              id={ids.controlId}
              role="group"
              aria-labelledby={ids.labelId}
              className="grid gap-2"
            >
              {steps.map((step, index) => (
                <DeliveryStepRow
                  // The position is the identity: steps have no id, and their order is their meaning.
                  key={index}
                  step={step}
                  index={index}
                  disabled={readOnly}
                  onChange={(changed) =>
                    set(steps.map((s, i) => (i === index ? changed : s)))
                  }
                  onRemove={() => set(steps.filter((_, i) => i !== index))}
                />
              ))}
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="justify-self-start"
                disabled={readOnly}
                onClick={() =>
                  set([
                    ...steps,
                    {
                      channel: "MAIL",
                      delayMinutes: 2 * 1440,
                      onlyIfUnacknowledged: true,
                    },
                  ])
                }
              >
                <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
                {t("delivery.addStep")}
              </Button>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
