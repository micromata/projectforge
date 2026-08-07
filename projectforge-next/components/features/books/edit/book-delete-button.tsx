"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useDeleteBook } from "./use-book-detail";
import type { BookDetail } from "../types";

interface Props {
  /** The book as the server delivered it — `markAsDeleted` needs the whole entity, not just its id. */
  book: BookDetail;
  disabled?: boolean;
}

/**
 * Marks the book as deleted after confirming, then returns to the list.
 *
 * Deliberately not part of [BookEditActions]: the confirmation carries its own state, and the
 * button only exists once the book has been saved at least once.
 */
export function BookDeleteButton({ book, disabled }: Props) {
  const router = useRouter();
  const t = useTranslations();
  const [confirming, setConfirming] = useState(false);
  const deleteBook = useDeleteBook();

  async function run() {
    const result = await deleteBook.mutateAsync(book);
    if (result.kind === "validationErrors") {
      // Nothing was deleted; the server explains why (e.g. the book is still lent out).
      result.validationErrors.forEach((error) => toast.error(error.message));
      return;
    }
    toast.success(t("message.successfullChanged"));
    router.push("/books");
  }

  return (
    <>
      <Button
        type="button"
        variant="destructive"
        size="sm"
        onClick={() => setConfirming(true)}
        disabled={disabled || deleteBook.isPending}
        className="gap-1.5"
      >
        <HugeiconsIcon icon={Delete01Icon} size={13} />
        {t("markAsDeleted")}
      </Button>
      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={t("markAsDeleted")}
        description={t("question.markAsDeletedQuestion")}
        confirmLabel={t("markAsDeleted")}
        destructive
        onConfirm={() => void run()}
      />
    </>
  );
}
