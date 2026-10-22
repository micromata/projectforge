"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import type {
  ContributionMarginBubbleGrouping,
  ContributionMarginBubbleSettings,
} from "@/lib/rs/order";

/**
 * What a bubble stands for (project, project group, customer, customer group), the switches for the project names
 * in the labels and for the anonymous bubbles of the projects the user may not see, with the size legend.
 */
export function ContributionMarginBubbleToolbar({
  value,
  onChange,
}: {
  value: ContributionMarginBubbleSettings;
  onChange: (value: ContributionMarginBubbleSettings) => void;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin.bubbles");
  // Spelled out: the generator exports only literal keys.
  const groupings: [ContributionMarginBubbleGrouping, string][] = [
    ["PROJECT", t("grouping.PROJECT")],
    ["PROJECT_GROUP", t("grouping.PROJECT_GROUP")],
    ["CUSTOMER", t("grouping.CUSTOMER")],
    ["CUSTOMER_GROUP", t("grouping.CUSTOMER_GROUP")],
  ];
  const groupingId = "contributionMarginBubbleGrouping";
  return (
    <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
      <div className="flex items-center gap-2">
        <HintTooltip text={t("grouping.tooltip")} openOnTap>
          <Label htmlFor={groupingId} className="whitespace-nowrap">
            {t("grouping._")}:
          </Label>
        </HintTooltip>
        <Select
          value={value.grouping}
          onValueChange={(grouping) =>
            onChange({
              ...value,
              grouping: grouping as ContributionMarginBubbleGrouping,
            })
          }
        >
          <SelectTrigger
            id={groupingId}
            className="h-8 w-auto"
            aria-label={t("grouping._")}
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {groupings.map(([key, label]) => (
              <SelectItem key={key} value={key}>
                {label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <ToolbarSwitch
        id="contributionMarginBubbleProjects"
        checked={value.showProjects}
        onChange={(showProjects) => onChange({ ...value, showProjects })}
        label={t("projects._")}
        tooltip={t("projects.tooltip")}
      />
      <ToolbarSwitch
        id="contributionMarginBubbleOthers"
        checked={value.includeOthers}
        onChange={(includeOthers) => onChange({ ...value, includeOthers })}
        label={t("others._")}
        tooltip={t("others.tooltip")}
      />
      <span className="text-muted-foreground">{t("size")}</span>
    </div>
  );
}

function ToolbarSwitch({
  id,
  checked,
  onChange,
  label,
  tooltip,
}: {
  id: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: string;
  tooltip: string;
}) {
  return (
    <div className="flex items-center gap-2">
      <Switch id={id} checked={checked} onCheckedChange={onChange} />
      <HintTooltip text={tooltip} openOnTap>
        <Label htmlFor={id}>{label}</Label>
      </HintTooltip>
    </div>
  );
}
