"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { SegmentedNumberField } from "@/components/shared/form/segmented-number-field";
import { formatKost2 } from "@/lib/number-ranges";
import { useNumberRanges } from "@/lib/rs/number-ranges";
import { kost2Segments } from "./cost2-number-segments";
import type { Cost2Values } from "./cost2-schema";

/**
 * The number of a cost unit: four boxes reading as one number, `6.100.01.02`.
 *
 * Labelled `fibu.kost.kostentraeger` like the fieldset of Wicket's edit form. The first three boxes
 * carry the generic cost labels; the last one is the `Kost2Art` (`fibu.kost2.art`) — a plain 0-99 box
 * in Wicket too, bound to `kost2Art.id` (the DTO carries it flat as `endziffer`).
 *
 * The boxes are the storage format of Kost2DO (1/3/2/2 digits). A number range may segment its Kost2
 * differently (e.g. `1.####.#.##`), so the hint shows the number as the range writes it.
 */
export function Cost2NumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const info = useNumberRanges();
  const value =
    (name: "nummernkreis" | "bereich" | "teilbereich" | "endziffer") =>
    (s: unknown) =>
      (s as { values: Cost2Values }).values[name];
  const nummernkreis = useStore(form.store, value("nummernkreis"));
  const bereich = useStore(form.store, value("bereich"));
  const teilbereich = useStore(form.store, value("teilbereich"));
  const endziffer = useStore(form.store, value("endziffer"));
  const segments = info?.ranges.find(
    (r) => r.prefix === nummernkreis
  )?.segments;
  const differs = segments != null && segments.join(",") !== "1,3,2,2";
  const label = (name: string) =>
    name === "endziffer" ? t("fibu.kost2.art") : t(`fibu.kost1.${name}`);
  return (
    <SegmentedNumberField
      label={t("fibu.kost.kostentraeger")}
      segments={kost2Segments(label)}
      separator="."
      hint={
        differs
          ? formatKost2(info, nummernkreis, bereich, teilbereich, endziffer)
          : undefined
      }
      className={className}
    />
  );
}
