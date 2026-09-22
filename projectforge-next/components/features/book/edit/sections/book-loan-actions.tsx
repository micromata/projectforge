"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowTurnBackwardIcon,
  BookOpen01Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { UserRef } from "../../types";

/**
 * Lend out / return, the two actions of the legacy `UICustomized("book.lendOutComponent")`
 * (BookLendOut.jsx).
 *
 * Both submit the form (see lib/rs/submit-meta.ts): the endpoints save the posted book as a side effect,
 * so the current form values have to go with them — a partial update doesn't exist here, and
 * sending anything else would let the page and the database drift apart.
 */
export function BookLoanActions() {
  const t = useTranslations("book");
  const form = useEntityEditForm();
  const { user } = useAuth();

  const id = useStore(form.store, (s: unknown) => (s as FormState).values.id);
  const lendOutBy = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.lendOutBy
  );
  const isSubmitting = useStore(
    form.store,
    (s: unknown) => (s as FormState).isSubmitting
  );

  // Only for a saved book, as in the legacy layout (`if (dto.id != null)`): lending out writes the
  // entity, and there is nothing to write before the first save.
  if (id == null) return null;

  // Who may return it is a client-side rule — the backend lets anyone with write access do it, and
  // the legacy page showed the button to the borrower only. Compared by id, not by username as the
  // legacy component did: UserRef carries no username (see types.ts).
  const mine = lendOutBy != null && user?.userId === lendOutBy.id;

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Deliberately always available, as in the legacy page: lending out a book that is already
          lent out reassigns it to the current user (the server takes the borrower from the session). */}
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={isSubmitting}
        onClick={() => void form.handleSubmit({ action: "lendOut" })}
        className="gap-1.5"
      >
        <HugeiconsIcon icon={BookOpen01Icon} size={13} />
        {t("lendOut")}
      </Button>
      {mine && (
        <Button
          type="button"
          variant="secondary"
          size="sm"
          disabled={isSubmitting}
          onClick={() => void form.handleSubmit({ action: "returnBook" })}
          className="gap-1.5"
        >
          <HugeiconsIcon icon={ArrowTurnBackwardIcon} size={13} />
          {t("returnBook")}
        </Button>
      )}
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  isSubmitting: boolean;
  values: { id: number | null; lendOutBy: UserRef | null };
}
