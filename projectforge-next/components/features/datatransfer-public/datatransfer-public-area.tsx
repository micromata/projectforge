"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon, Logout01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { LogoRow } from "@/components/shared/logo-row";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { AttachmentServiceProvider } from "@/components/shared/attachments/attachment-service";
import { useDocumentTitle } from "@/hooks/use-document-title";
import {
  dataTransferPublicDownloadAllUrl,
  dataTransferPublicStateQueryKey,
  publicAttachmentService,
  PUBLIC_DATATRANSFER_ENTITY,
  type DataTransferPublicState,
} from "@/lib/rs/datatransfer-public";

/**
 * The files of an area as an external user sees them, once logged in: all of them with download
 * access, otherwise only those uploaded in this session. Upload, rename and delete only where the
 * area allows uploads, and only for one's own files (the backend marks the others read-only); these can
 * be encrypted as well.
 */
export function DataTransferPublicArea({
  token,
  state,
  onLogout,
}: {
  token: string;
  state: DataTransferPublicState;
  onLogout: () => void;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const id = state.id!;
  const downloadEnabled = state.externalDownloadEnabled === true;
  const uploadEnabled = state.externalUploadEnabled === true;
  const maxUploadSize = state.maxUploadSize;
  const maxUploadSizeFormatted = state.maxUploadSizeFormatted ?? "";
  const maxZipSize = state.maxZipSize;
  const maxZipSizeFormatted = state.maxZipSizeFormatted ?? "";
  const userInfo = state.userInfoVisible === true;
  // Built from the single values rather than the state, which is a new object after every upload.
  const service = useMemo(
    () =>
      publicAttachmentService(
        token,
        {
          multiDownload: downloadEnabled,
          maxFileSize:
            maxUploadSize == null
              ? undefined
              : { bytes: maxUploadSize, formatted: maxUploadSizeFormatted },
          maxZipSize:
            maxZipSize == null
              ? undefined
              : { bytes: maxZipSize, formatted: maxZipSizeFormatted },
          userInfo,
        },
        queryClient
      ),
    [
      token,
      downloadEnabled,
      maxUploadSize,
      maxUploadSizeFormatted,
      maxZipSize,
      maxZipSizeFormatted,
      userInfo,
      queryClient,
    ]
  );
  useDocumentTitle(state.areaName ?? t("plugins.datatransfer.title.heading"));

  return (
    <div className="flex min-h-screen flex-col bg-muted/40">
      <LogoRow collapsible={false} />
      <BrandStripe />
      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 px-4 py-6">
        <div className="flex flex-wrap items-start gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-xs text-muted-foreground">
              {t("plugins.datatransfer.title.heading")}
            </p>
            <h1 className="text-xl font-semibold break-words">
              {state.areaName}
            </h1>
          </div>
          {state.downloadAllAvailable && (
            <HintTooltip
              text={t("plugins.datatransfer.button.downloadAll.info")}
            >
              <Button asChild variant="outline" size="sm" className="gap-1.5">
                {/* A plain anchor: the ZIP is a download the browser handles, not a route. */}
                <a href={dataTransferPublicDownloadAllUrl(id)} download>
                  <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
                  {t("plugins.datatransfer.button.downloadAll._")}
                </a>
              </Button>
            </HintTooltip>
          )}
          <Button
            variant="outline"
            size="sm"
            className="gap-1.5"
            onClick={onLogout}
          >
            <HugeiconsIcon icon={Logout01Icon} size={14} aria-hidden />
            {t("menu.logout")}
          </Button>
        </div>
        {state.description && (
          <p className="text-sm whitespace-pre-wrap text-muted-foreground">
            {state.description}
          </p>
        )}
        <div className="rounded-lg border bg-background p-4">
          <AttachmentServiceProvider service={service}>
            <AttachmentList
              entity={PUBLIC_DATATRANSFER_ENTITY}
              id={id}
              readOnly={!uploadEnabled}
              layout="table"
              // As in the area itself, but kept in the browser: an external visitor has no prefs.
              columnStates="datatransferfiles-public"
              columnStatesStorage="local"
              uploadHint={
                uploadEnabled
                  ? `${t("plugins.datatransfer.maxUploadSize._")}: ${
                      state.maxUploadSizeFormatted ?? ""
                    }`
                  : undefined
              }
              // The "download all" depends on the files.
              onChanged={() =>
                void queryClient.invalidateQueries({
                  queryKey: dataTransferPublicStateQueryKey(token),
                })
              }
            />
          </AttachmentServiceProvider>
        </div>
      </main>
    </div>
  );
}
