"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { InformationCircleIcon, RefreshIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { CopyableValue } from "@/components/shared/copyable-value";
import { FieldHint } from "@/components/shared/form/field-hint";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { RsError } from "@/lib/rs/client";
import { MY_ACCOUNT_QUERY_KEY, renewMyAccountToken } from "@/lib/rs/my-account";
import { toast } from "@/lib/toast";
import { TokenInfoDialog } from "./token-info-dialog";
import type {
  MyAccountInit,
  MyAccountToken,
  MyAccountTokenType,
} from "./types";

/**
 * The access tokens of the user (calendar export, DAV, REST client): hidden until revealed, copyable,
 * renewable after a confirmation, and with their usage in a dialog.
 */
export function MyAccountTokens({
  tokens,
  canRenew,
}: {
  tokens: MyAccountToken[];
  /** False for a restricted or demo user, who may not renew a token: the button is left out. */
  canRenew: boolean;
}) {
  // On a wide screen one line per token, its columns aligned across the tokens (a subgrid per row): name,
  // value, last change, buttons. Narrower, each token is stacked.
  return (
    <div className="flex flex-col gap-5 lg:grid lg:grid-cols-[max-content_minmax(12rem,1fr)_max-content_max-content] lg:items-center lg:gap-x-3 lg:gap-y-3">
      {tokens.map((token) => (
        <TokenRow key={token.type} token={token} canRenew={canRenew} />
      ))}
    </div>
  );
}

function useTokenTexts(type: MyAccountTokenType): {
  label: string;
  hint: string;
} {
  const t = useTranslations();
  switch (type) {
    case "CALENDAR_REST":
      return {
        label: t("user.authenticationToken.calendar_rest._"),
        hint: t("user.authenticationToken.calendar_rest.tooltip"),
      };
    case "DAV_TOKEN":
      return {
        label: t("user.authenticationToken.dav_token._"),
        hint: t("user.authenticationToken.dav_token.tooltip"),
      };
    case "REST_CLIENT":
      return {
        label: t("user.authenticationToken.rest_client._"),
        hint: t("user.authenticationToken.rest_client.tooltip"),
      };
  }
}

function TokenRow({
  token,
  canRenew,
}: {
  token: MyAccountToken;
  canRenew: boolean;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const { label, hint } = useTokenTexts(token.type);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [infoOpen, setInfoOpen] = useState(false);
  const [renewing, setRenewing] = useState(false);

  const renew = async () => {
    setRenewing(true);
    try {
      const renewed = await renewMyAccountToken(token.type);
      // Only the token: the rest of the page may hold unsaved edits, and a refetch would leave them be
      // anyway, but there is nothing else that changed.
      queryClient.setQueryData<MyAccountInit>(MY_ACCOUNT_QUERY_KEY, (old) =>
        old
          ? {
              ...old,
              data: {
                ...old.data,
                tokens: old.data.tokens?.map((it) =>
                  it.type === renewed.type ? renewed : it
                ),
              },
            }
          : old
      );
      toast.success(t("user.authenticationToken.renew.successful"));
    } catch (error) {
      toast.error(error instanceof RsError ? error.message : String(error));
    } finally {
      setRenewing(false);
    }
  };

  const lastChangeLabel = t("user.authenticationToken.lastChange._");
  return (
    <div className="flex flex-col gap-1.5 lg:col-span-4 lg:grid lg:grid-cols-subgrid lg:items-center">
      <div className="flex items-start gap-1">
        <Label className="text-[11.5px] font-semibold tracking-wide whitespace-nowrap text-muted-foreground uppercase">
          {label}
        </Label>
        <FieldHint hint={hint} label={label} />
      </div>
      {/* The beginning shows through and fades out, as with the passwords (see secretPeek). */}
      <CopyableValue value={token.token} label={label} masked peek />
      <span className="flex items-start gap-1 text-xs text-muted-foreground">
        <span>
          {lastChangeLabel}: {token.creationDateFormatted || "–"}
        </span>
        <FieldHint
          hint={t("user.authenticationToken.lastChange.tooltip")}
          label={lastChangeLabel}
        />
      </span>
      <div className="flex items-center gap-2">
        {canRenew && (
          <HintTooltip text={t("user.authenticationToken.renew.tooltip")}>
            <Button
              type="button"
              variant="outline"
              className="shrink-0 text-destructive"
              disabled={renewing}
              onClick={() => setConfirmOpen(true)}
            >
              <HugeiconsIcon icon={RefreshIcon} size={14} aria-hidden />
              {t("user.authenticationToken.renew._")}
            </Button>
          </HintTooltip>
        )}
        <HintTooltip
          text={t("user.authenticationToken.button.showUsage.tooltip")}
        >
          <Button
            type="button"
            variant="outline"
            className="shrink-0"
            onClick={() => setInfoOpen(true)}
          >
            <HugeiconsIcon icon={InformationCircleIcon} size={14} aria-hidden />
            {t("user.authenticationToken.button.showUsage._")}
          </Button>
        </HintTooltip>
      </div>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={`${label}: ${t("user.authenticationToken.renew._")}`}
        description={t("user.authenticationToken.renew.securityQuestion")}
        confirmLabel={t("user.authenticationToken.renew._")}
        destructive
        onConfirm={() => void renew()}
      />
      {infoOpen && (
        <TokenInfoDialog
          type={token.type}
          label={label}
          onClose={() => setInfoOpen(false)}
        />
      )}
    </div>
  );
}
