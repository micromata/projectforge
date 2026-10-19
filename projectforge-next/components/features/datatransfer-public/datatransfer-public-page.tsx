"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AuthCard } from "@/components/shared/auth-card";
import { FormAlert } from "@/components/shared/form-alert";
import {
  dataTransferPublicStateQueryKey,
  fetchDataTransferPublicState,
  logoutDataTransferPublic,
  type DataTransferPublicState,
} from "@/lib/rs/datatransfer-public";
import { DataTransferPublicArea } from "./datatransfer-public-area";
import { DataTransferPublicLogin } from "./datatransfer-public-login";

/**
 * The external access of a data transfer area (`/next/public/datatransfer/<token>`), for someone
 * without a ProjectForge account: the login with the area's password, then its files.
 *
 * The legacy page (`/react/public/datatransfer/dynamic/<token>`) still runs in parallel, and the link
 * the area's owners pass on still points there.
 */
export function DataTransferPublicPage({ token }: { token: string }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const queryKey = dataTransferPublicStateQueryKey(token);
  const { data, isPending, isError } = useQuery<DataTransferPublicState>({
    queryKey,
    queryFn: ({ signal }) => fetchDataTransferPublicState(token, signal),
    retry: false,
  });
  const title = data?.areaName ?? t("plugins.datatransfer.title.heading");

  if (isPending) {
    return (
      <AuthCard title={t("plugins.datatransfer.title.heading")}>
        <div className="flex justify-center py-6">
          <div className="size-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
        </div>
      </AuthCard>
    );
  }
  if (isError) {
    return (
      <AuthCard title={t("plugins.datatransfer.title.heading")}>
        <FormAlert tone="error">{t("validation.error.generic")}</FormAlert>
      </AuthCard>
    );
  }
  if (!data.loggedIn) {
    return (
      <DataTransferPublicLogin
        token={token}
        title={title}
        notice={notice}
        onLoggedIn={(state) => {
          setNotice(null);
          queryClient.setQueryData(queryKey, state);
        }}
      />
    );
  }
  return (
    <DataTransferPublicArea
      token={token}
      state={data}
      onLogout={async () => {
        try {
          setNotice(await logoutDataTransferPublic());
        } catch {
          // The session may be gone already; the state below tells.
        }
        // The files of the area must not survive the logout in the cache.
        queryClient.removeQueries({ queryKey: ["attachments"] });
        await queryClient.invalidateQueries({ queryKey });
      }}
    />
  );
}
