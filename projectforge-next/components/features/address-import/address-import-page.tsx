"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import { FormAlert } from "@/components/shared/form-alert";
import { ImportStatisticsLine } from "@/components/shared/import/import-statistics-line";
import { MarkdownText } from "@/components/shared/markdown-text";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import {
  ADDRESS_IMPORT_QUERY_KEY,
  cancelAddressImport,
  fetchAddressImport,
  uploadAddressImport,
} from "@/lib/rs/address-import";
import { AddressImportTable } from "./address-import-table";

/**
 * The VCF import of addresses (`/address/import`), successor of the React page `AddressImportUploadPageRest`.
 *
 * A dropped file is matched against the address book at once; the list then shows each vCard as new, changed or
 * unchanged. There is no "import all": an entry is taken over by opening it in the address form, comparing its
 * fields there and saving — after which it reads "imported" here. The import is kept in the session for half an
 * hour.
 */
export function AddressImportPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [progress, setProgress] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const state = useQuery({
    queryKey: ADDRESS_IMPORT_QUERY_KEY,
    queryFn: ({ signal }) => fetchAddressImport(signal),
  });
  const upload = useMutation({
    mutationFn: (file: File) =>
      uploadAddressImport(file, {
        onProgress: (p) => setProgress(p.percent ?? 0),
      }),
    onMutate: () => {
      setError(null);
      setProgress(0);
    },
    onSettled: () => setProgress(null),
    onSuccess: (result) => {
      if (result.kind === "rejected") {
        setError(result.error);
        return;
      }
      queryClient.setQueryData(ADDRESS_IMPORT_QUERY_KEY, result.view);
    },
    onError: (err) =>
      setError(err instanceof Error ? err.message : String(err)),
  });
  const cancel = useMutation({
    mutationFn: cancelAddressImport,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ADDRESS_IMPORT_QUERY_KEY }),
  });
  const view = state.data;
  const hasImport = !!view?.filename;

  return (
    <PageShell>
      <PageTitleRow
        category={t("address.title.list")}
        title={t("address.book.vCardsImport.title")}
      >
        <Button asChild variant="outline">
          <Link href="/address">
            <HugeiconsIcon icon={ArrowLeft01Icon} />
            {t("address.title.list")}
          </Link>
        </Button>
        {hasImport && (
          <Button
            type="button"
            variant="outline"
            disabled={cancel.isPending}
            onClick={() => cancel.mutate()}
          >
            {t("cancel")}
          </Button>
        )}
      </PageTitleRow>
      <div className="flex min-h-0 flex-1 flex-col gap-3 px-4 py-3">
        {state.isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner className="h-6 w-6 border-2" />
          </div>
        ) : hasImport ? (
          <>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
              <span className="font-semibold">{view.filename}</span>
              <ImportStatisticsLine info={view.info} />
            </div>
            <p className="text-sm text-muted-foreground">
              {t("address.book.vCardsImport.clickToEdit")}
            </p>
            <AddressImportTable entries={view.entries} />
          </>
        ) : (
          <div className="flex max-w-2xl flex-col gap-3">
            <MarkdownText text={t("address.book.vCardsImport.description")} />
            <AttachmentDropArea
              onFiles={(files) => files[0] && upload.mutate(files[0])}
              accept=".vcf"
              multiple={false}
              disabled={upload.isPending}
              label={t("address.book.vCardsImport.dropHint")}
            />
            {progress !== null && <Progress value={progress} />}
          </div>
        )}
        {error && <FormAlert tone="error">{error}</FormAlert>}
      </div>
    </PageShell>
  );
}
