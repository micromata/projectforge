"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { RefreshIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { CopyableValue } from "@/components/shared/copyable-value";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { SecretField } from "@/components/shared/form/secret-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { Label } from "@/components/ui/label";
import {
  renewDataTransferAccessToken,
  renewDataTransferPassword,
} from "@/lib/rs/datatransfer";

interface Props {
  /** The token of the form, possibly renewed and not yet saved. */
  token: string | null;
  /** The link and token as loaded: the link's base is the part before the token. */
  storedLink?: string | null;
  storedToken?: string | null;
}

/**
 * The password and the link of the external access, each with a "renew" — a leaked link or password is
 * replaced here, and the old one is invalid once the form is saved. The password is hidden until
 * revealed, and both can be copied (see SecretField, CopyableValue).
 *
 * The link is the public page plus the token (`DataTransferArea.externalLink`); the backend sends it for
 * the stored token only, so a renewed token is put behind the same base here.
 */
export function DataTransferExternalSecrets({
  token,
  storedLink,
  storedToken,
}: Props) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const base =
    storedLink && storedToken && storedLink.endsWith(storedToken)
      ? storedLink.slice(0, storedLink.length - storedToken.length)
      : null;
  const link = base != null && token ? `${base}${token}` : null;
  const linkLabel = t("plugins.datatransfer.external.link._");

  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <SecretField
        name="externalPassword"
        label={t("plugins.datatransfer.external.password._")}
        hint={t("plugins.datatransfer.external.password.info")}
        peek
      >
        <RenewButton
          label={t("plugins.datatransfer.external.password.renew._")}
          hint={t("plugins.datatransfer.external.password.renew.info")}
          onRenew={async () =>
            form.setFieldValue(
              "externalPassword",
              await renewDataTransferPassword()
            )
          }
        />
      </SecretField>
      <div className="flex flex-col gap-2 sm:col-span-2">
        <Label>{linkLabel}</Label>
        <CopyableValue value={link} label={linkLabel}>
          <RenewButton
            label={t("plugins.datatransfer.external.link.renew._")}
            hint={t("plugins.datatransfer.external.link.renew.info")}
            onRenew={async () =>
              form.setFieldValue(
                "externalAccessToken",
                await renewDataTransferAccessToken()
              )
            }
          />
        </CopyableValue>
      </div>
    </div>
  );
}

function RenewButton({
  label,
  hint,
  onRenew,
}: {
  label: string;
  hint: string;
  onRenew: () => Promise<void>;
}) {
  return (
    <HintTooltip text={hint}>
      <Button
        type="button"
        variant="outline"
        className="shrink-0 text-destructive"
        onClick={() => void onRenew()}
      >
        <HugeiconsIcon icon={RefreshIcon} size={14} aria-hidden />
        {label}
      </Button>
    </HintTooltip>
  );
}
