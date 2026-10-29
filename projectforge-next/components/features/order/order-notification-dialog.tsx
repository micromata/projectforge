"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  fetchOrderNotificationSettings,
  ORDER_NOTIFICATION_QUERY_KEY,
  saveOrderNotificationSettings,
  type OrderNotificationRule,
  type OrderNotificationSettings,
} from "@/lib/rs/order-notification";
import { toast } from "@/lib/toast";
import { OrderNotificationsEditor } from "./order-notifications-editor";

type Suggestions = Pick<OrderNotificationRule, "type" | "id" | "displayName">[];

/**
 * The user's order notification settings in a dialog: changes are collected in a draft and only saved with
 * "Save"; "Cancel" (or closing the dialog) throws them away.
 */
export function OrderNotificationDialog({
  suggestions,
  onClose,
}: {
  suggestions?: Suggestions;
  onClose: () => void;
}) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ORDER_NOTIFICATION_QUERY_KEY,
    queryFn: ({ signal }) => fetchOrderNotificationSettings(signal),
  });
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="flex max-h-[90vh] flex-col sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            {t("fibu.auftrag.notification.prefs.title")}
          </DialogTitle>
        </DialogHeader>
        {query.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {!query.isPending && !query.data && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {query.data && (
          <Draft
            initial={query.data}
            suggestions={suggestions}
            onClose={onClose}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}

function Draft({
  initial,
  suggestions,
  onClose,
}: {
  initial: OrderNotificationSettings;
  suggestions?: Suggestions;
  onClose: () => void;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState(initial);
  const [saving, setSaving] = useState(false);
  const writable = initial.writeAccess !== false;

  const save = async () => {
    setSaving(true);
    try {
      const saved = await saveOrderNotificationSettings(draft);
      queryClient.setQueryData(ORDER_NOTIFICATION_QUERY_KEY, saved);
      onClose();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
      setSaving(false);
    }
  };

  return (
    <>
      {/* Only the rules scroll; the buttons and the note below them stay in view. */}
      <div className="-mx-1 min-h-0 flex-1 overflow-y-auto px-1">
        <OrderNotificationsEditor
          settings={draft}
          onChange={setDraft}
          editable={writable && !saving}
          suggestions={suggestions}
        />
      </div>
      <DialogFooter className="shrink-0">
        <Button variant="outline" onClick={onClose}>
          {t("cancel")}
        </Button>
        <Button disabled={!writable || saving} onClick={() => void save()}>
          {t("save")}
        </Button>
      </DialogFooter>
      <p className="shrink-0 text-xs text-muted-foreground">
        {t("fibu.auftrag.notification.prefs.precedence")}
      </p>
    </>
  );
}
