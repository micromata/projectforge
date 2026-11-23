"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { setNetworkErrorText } from "@/lib/rs/client";

/** Hands the translated "server not reachable" text to the request layer (lib/rs/client.ts). */
export function NetworkErrorText() {
  const t = useTranslations();
  useEffect(() => {
    setNetworkErrorText(() => t("plugin.frontend.error.network"));
    return () => setNetworkErrorText(null);
  }, [t]);
  return null;
}
