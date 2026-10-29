"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchBusinessCardPreview } from "@/lib/rs/my-business-cards";
import type { BusinessCard } from "./types";

/** Waiting time after the last change before the QR code is rebuilt. */
const PREVIEW_DELAY_MS = 400;

/**
 * The QR code (and whether the photo may be published) of the card as it is being edited, rebuilt shortly
 * after the last change. The previous answer stays on screen meanwhile.
 */
export function useBusinessCardPreview(card: BusinessCard) {
  const [debounced, setDebounced] = useState(card);
  const key = JSON.stringify(card);
  useEffect(() => {
    const timer = setTimeout(
      () => setDebounced(JSON.parse(key)),
      PREVIEW_DELAY_MS
    );
    return () => clearTimeout(timer);
  }, [key]);
  return useQuery({
    queryKey: ["myBusinessCards", "preview", debounced],
    queryFn: ({ signal }) => fetchBusinessCardPreview(debounced, signal),
    placeholderData: (previous) => previous,
  });
}
