"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import { toast } from "@/lib/toast";
import { deleteBusinessCard } from "@/lib/rs/my-business-cards";
import type { BusinessCard } from "./types";

/** Deletes a stored card after asking; the right-aligned destructive button of the editor's action bar. */
export function BusinessCardDeleteButton({
  card,
  onDeleted,
}: {
  card: BusinessCard;
  onDeleted: () => void;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const remove = async () => {
    setOpen(false);
    try {
      await deleteBusinessCard(card.id!);
      onDeleted();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    }
  };
  return (
    <>
      <Button type="button" variant="destructive" onClick={() => setOpen(true)}>
        {t("delete")}
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={setOpen}
        title={t("delete")}
        description={
          <>
            {t("businessCard.delete.question")} {card.cardName}
          </>
        }
        confirmLabel={t("delete")}
        destructive
        onConfirm={() => void remove()}
      />
    </>
  );
}
