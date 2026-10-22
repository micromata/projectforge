"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { MassUpdateParameter } from "@/lib/rs/multi-select";

/**
 * Adopts the values of the comparison campaign the list shows (e.g. last year's) for the selected rows — each row
 * the value its address has there (`AddressCampaignValueMassUpdateRest.Adoption`). Optionally the comments
 * as well (appended to a row's own) and only for rows without a value of their own.
 *
 * A custom field because it has no entity property behind it. The choices travel as the comma separated
 * `textValue` of the `adoptComparison` param (`value`, `comment`, `onlyEmpty`); unticked, the field posts nothing.
 */
export function AddressCampaignValueAdoptMassUpdateField({
  field,
  comparisonTitle,
  setParam,
}: {
  field: string;
  comparisonTitle: string | null | undefined;
  setParam: (name: string, param: MassUpdateParameter | undefined) => void;
}) {
  const t = useTranslations();
  const id = useId();
  const [adopt, setAdopt] = useState(false);
  const [comment, setComment] = useState(true);
  const [onlyEmpty, setOnlyEmpty] = useState(true);

  useEffect(() => {
    const options = ["value"];
    if (comment) options.push("comment");
    if (onlyEmpty) options.push("onlyEmpty");
    setParam(field, adopt ? { textValue: options.join(",") } : undefined);
    // `setParam` is stable (a useCallback in MassUpdateForm); the ticks drive the param.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adopt, comment, onlyEmpty, field]);

  const option = (
    key: string,
    checked: boolean,
    onChange: (checked: boolean) => void,
    label: string,
    disabled = false
  ) => (
    <div className="flex items-center gap-2">
      <Checkbox
        id={`${id}-${key}`}
        checked={checked}
        disabled={disabled}
        onCheckedChange={(value) => onChange(value === true)}
      />
      <Label htmlFor={`${id}-${key}`} className="text-sm font-normal">
        {label}
      </Label>
    </div>
  );

  return (
    <div className="space-y-2">
      {option(
        "adopt",
        adopt,
        setAdopt,
        comparisonTitle
          ? `${t("plugins.marketing.addressCampaignValue.adoptValues")}: ${comparisonTitle}`
          : t("plugins.marketing.addressCampaignValue.adoptValues")
      )}
      <div className="space-y-2 pl-6">
        {option(
          "comment",
          comment,
          setComment,
          t("plugins.marketing.addressCampaignValue.adoptComments"),
          !adopt
        )}
        {option(
          "onlyEmpty",
          onlyEmpty,
          setOnlyEmpty,
          t("plugins.marketing.addressCampaignValue.adoptOnlyEmpty"),
          !adopt
        )}
      </div>
    </div>
  );
}
