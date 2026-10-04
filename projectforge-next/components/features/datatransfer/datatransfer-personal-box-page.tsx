"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SectionCard } from "@/components/shared/section-card";
import { toast } from "@/lib/toast";
import { fetchPersonalBox } from "@/lib/rs/datatransfer";

/**
 * Opens another user's personal box (`/datatransfer/personal-box`): the way to send a single colleague
 * a file. The backend creates the box if the user has none yet, and remembers the choice, which is why
 * the last user is preset here.
 */
export function DataTransferPersonalBoxPage() {
  const t = useTranslations();
  const router = useRouter();
  const last = useQuery({
    queryKey: ["datatransfer", "personalBox", "last"],
    queryFn: ({ signal }) => fetchPersonalBox(undefined, signal),
  });
  const [picked, setPicked] = useState<EntityRef | null>(null);
  const lastUser = last.data?.user;
  const user =
    picked ??
    (lastUser
      ? { id: lastUser.id, displayName: lastUser.displayName ?? "" }
      : null);

  const open = useMutation({
    mutationFn: (userId: number) => fetchPersonalBox(userId),
    onSuccess: (box) => {
      // Without `/next`: the router adds the base path itself.
      if (box.boxId != null) router.push(`/datatransfer/${box.boxId}`);
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("plugins.datatransfer.title.heading")}
        title={t("plugins.datatransfer.personalBox._")}
      />
      <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-4">
        <SectionCard className="flex max-w-xl flex-col gap-3">
          <Label htmlFor="datatransfer-personal-box-user">
            {t("plugins.datatransfer.personalBox.select")}
          </Label>
          <div className="flex items-center gap-2">
            <EntityAutocomplete
              id="datatransfer-personal-box-user"
              url="user/autosearch?search=:search"
              value={user}
              onChange={(value) => {
                setPicked(value);
                if (value) open.mutate(value.id);
              }}
              className="flex-1"
              autoFocus
            />
            <Button
              type="button"
              disabled={!user || open.isPending}
              onClick={() => user && open.mutate(user.id)}
            >
              {t("show")}
            </Button>
          </div>
        </SectionCard>
      </div>
    </PageShell>
  );
}
