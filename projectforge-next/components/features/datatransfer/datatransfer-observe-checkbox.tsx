"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { toast } from "@/lib/toast";
import {
  dataTransferViewQueryKey,
  observeDataTransferArea,
} from "@/lib/rs/datatransfer";

/**
 * Whether the logged-in user is notified of the area's activities. Anyone with access may observe an
 * area, not only its admins — which is why this is a call of its own and not a field of the admin form.
 * The answer is the new view, observers included, and replaces the cached one.
 */
export function DataTransferObserveCheckbox({
  id,
  checked,
}: {
  id: number;
  checked: boolean;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: (observe: boolean) => observeDataTransferArea(id, observe),
    onSuccess: (view) =>
      queryClient.setQueryData(dataTransferViewQueryKey(id), view),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  const inputId = `datatransfer-observe-${id}`;

  return (
    <HintTooltip text={t("plugins.datatransfer.userWantsToObserve.info")}>
      <div className="flex items-center gap-2">
        <Checkbox
          id={inputId}
          checked={mutation.isPending ? mutation.variables : checked}
          disabled={mutation.isPending}
          onCheckedChange={(value) => mutation.mutate(value === true)}
        />
        <Label htmlFor={inputId}>
          {t("plugins.datatransfer.userWantsToObserve._")}
        </Label>
      </div>
    </HintTooltip>
  );
}
