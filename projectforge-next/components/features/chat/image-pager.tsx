"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";

/** Previous / next buttons with a "2 / 5" counter for the images of a message. */
export function ImagePager({
  index,
  count,
  onChange,
}: {
  index: number;
  count: number;
  onChange: (index: number) => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex items-center gap-2">
      <Button
        type="button"
        variant="outline"
        size="icon-sm"
        aria-label={t("chat.file.previous")}
        disabled={index <= 0}
        onClick={() => onChange(index - 1)}
      >
        <HugeiconsIcon icon={ArrowLeft01Icon} />
      </Button>
      <span className="text-xs text-muted-foreground tabular-nums">
        {index + 1} / {count}
      </span>
      <Button
        type="button"
        variant="outline"
        size="icon-sm"
        aria-label={t("chat.file.next")}
        disabled={index >= count - 1}
        onClick={() => onChange(index + 1)}
      >
        <HugeiconsIcon icon={ArrowRight01Icon} />
      </Button>
    </div>
  );
}
