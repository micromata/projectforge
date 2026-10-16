"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { StarIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { setAddressFavorite } from "@/lib/rs/address";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import { ADDRESS_LIST_QUERY_KEY } from "./address-routes";

interface Props {
  addressId: number;
  favorite: boolean;
  /** Called with the new state once the backend took it. */
  onChanged?: (favorite: boolean) => void;
  className?: string;
}

/**
 * The star of an address: whether it is one of the logged-in user's favorites — the ones CardDAV syncs and the
 * vCard export exports. A click toggles it at once, without a save of the address (`address/favorite`).
 */
export function AddressFavoriteToggle({
  addressId,
  favorite,
  onChanged,
  className,
}: Props) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const toggle = useMutation({
    mutationFn: () => setAddressFavorite(addressId, !favorite),
    onSuccess: ({ isFavoriteCard }) => {
      onChanged?.(isFavoriteCard);
      void queryClient.invalidateQueries({ queryKey: ADDRESS_LIST_QUERY_KEY });
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      className={cn("size-7", className)}
      aria-pressed={favorite}
      aria-label={t(leafKeyOf("favorite", t.has))}
      disabled={toggle.isPending}
      onClick={(event) => {
        // In a list row: the click toggles the star and does not open the address.
        event.stopPropagation();
        toggle.mutate();
      }}
    >
      <HugeiconsIcon
        icon={StarIcon}
        size={16}
        className={cn(
          favorite
            ? "fill-current text-brand-yellow-dark"
            : "text-muted-foreground"
        )}
      />
    </Button>
  );
}
