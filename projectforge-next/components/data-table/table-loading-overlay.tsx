import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";

/**
 * Says that the rows on screen are not the answer to the question just asked — the table keeps the
 * previous page while the new one is fetched (`keepPreviousData` in useMagicFilterQuery), so without
 * this a changed filter or page looks like a result that simply didn't change.
 *
 * `pointer-events-none` and a translucent backdrop, not a modal: the old rows stay readable and the
 * toolbar stays usable, which matters most where the answer is slow — that is exactly when the user
 * wants to correct the filter again.
 *
 * Appears only if the wait is noticeable: `delay-300` with `fill-mode-both` holds the animation's
 * `opacity: 0` during the delay, so a fast response never flashes it. Done in CSS rather than with a
 * timer, because a state update would then have to be cancelled on every quick refetch.
 */
export function TableLoadingOverlay() {
  const t = useTranslations();
  return (
    <div className="pointer-events-none absolute inset-0 z-30 flex animate-in items-center justify-center bg-background/50 fade-in fill-mode-both delay-300 duration-200">
      <div className="flex items-center gap-2 rounded-md border bg-background px-3 py-2 shadow-sm">
        <Spinner className="h-4 w-4 border-2" />
        <span className="text-xs text-muted-foreground">{t("loading")}</span>
      </div>
    </div>
  );
}
