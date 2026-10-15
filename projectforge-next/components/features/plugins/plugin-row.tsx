"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { setPluginActivated } from "@/lib/rs/plugins";
import { toast } from "@/lib/toast";
import type { PluginItem } from "./types";

/**
 * One plugin row: its id, name and description, plus a button to activate or deactivate it. The change
 * only takes effect after a restart, so the mutation toasts the backend's note and refreshes the list.
 * A plugin forced active via `projectforge.plugins.ensure-active` cannot be deactivated — its button is
 * disabled with an explaining tooltip.
 */
export function PluginRow({ plugin }: { plugin: PluginItem }) {
  const t = useTranslations();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => setPluginActivated(plugin.id, !plugin.active),
    onSuccess: (res) => {
      if (res.message) toast.success(res.message);
      void queryClient.invalidateQueries({ queryKey: ["plugins"] });
    },
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });

  const disabled = mutation.isPending || (plugin.active && plugin.ensureActive);

  const button = (
    <Button
      variant={plugin.active ? "destructive" : "default"}
      size="sm"
      disabled={disabled}
      onClick={() => mutation.mutate()}
      aria-label={t(
        plugin.active
          ? "system.pluginAdmin.button.deactivate"
          : "system.pluginAdmin.button.activate"
      )}
    >
      {t(
        plugin.active
          ? "system.pluginAdmin.button.deactivate"
          : "system.pluginAdmin.button.activate"
      )}
    </Button>
  );

  return (
    <div className="flex items-center gap-4 px-4 py-3">
      <div className="min-w-0 flex-1">
        <p className="font-medium">{plugin.name || plugin.id}</p>
        <p className="text-xs text-muted-foreground">{plugin.id}</p>
        {plugin.description && (
          <p className="mt-1 text-sm text-muted-foreground">
            {plugin.description}
          </p>
        )}
      </div>
      {plugin.active && plugin.ensureActive ? (
        <HintTooltip text={t("system.pluginAdmin.ensureActiveHint")} openOnTap>
          {/* A disabled button emits no hover events, so the span carries the trigger. */}
          <span className="inline-flex">{button}</span>
        </HintTooltip>
      ) : (
        button
      )}
    </div>
  );
}
