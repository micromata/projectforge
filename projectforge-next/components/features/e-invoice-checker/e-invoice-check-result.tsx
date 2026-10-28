"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatDate,
  formatNumber,
  type FormatContext,
} from "@/lib/format";
import {
  downloadEInvoiceAttachment,
  type EInvoiceCheckResult,
  type EInvoiceParty,
} from "@/lib/rs/e-invoice-checker";
import { toast } from "@/lib/toast";

/**
 * What the checker read from one file: validity first (the reason one checks), then the invoice data, the
 * parties, the amounts, the line items and the embedded attachments.
 *
 * Amounts are shown in the invoice's own currency, not in the user's: it is the file's statement that is
 * being checked.
 */
export function EInvoiceCheckResultView({
  result,
}: {
  result: EInvoiceCheckResult;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const data = result.invoiceData;
  const currencyCtx: FormatContext = data.currency
    ? { ...ctx, currency: data.currency }
    : ctx;
  const amount = (value: number | null | undefined) =>
    formatCurrency(value, currencyCtx);
  const day = (value: string | null | undefined) =>
    formatDate(value, ctx) || value;

  return (
    <div className="flex flex-col gap-4" data-testid="e-invoice-check-result">
      <div className="flex flex-col gap-2">
        <p className="text-sm font-medium">{result.filename}</p>
        {data.validationErrors.length === 0 ? (
          <FormAlert tone="success">
            {t("fibu.eInvoiceChecker.valid")}
          </FormAlert>
        ) : (
          <FormAlert tone="error">
            <p>{t("fibu.eInvoiceChecker.invalid")}</p>
            <ul className="mt-1 list-disc pl-5">
              {data.validationErrors.map((error, index) => (
                <li key={index}>{error}</li>
              ))}
            </ul>
          </FormAlert>
        )}
        {data.warnings.map((warning, index) => (
          <FormAlert key={index} tone="warning">
            {warning}
          </FormAlert>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <InfoCard title={t("fibu.eInvoiceChecker.metadata")}>
          <InfoItem label={t("fibu.rechnung.nummer._")}>
            {data.invoiceNumber}
          </InfoItem>
          <InfoItem label={t("fibu.rechnung.datum._")}>
            {day(data.issueDate)}
          </InfoItem>
          <InfoItem label={t("fibu.rechnung.faelligkeit")}>
            {day(data.dueDate)}
          </InfoItem>
          <InfoItem label={t("fibu.rechnung.currency")}>
            {data.currency}
          </InfoItem>
          <InfoItem label={t("fibu.eInvoiceChecker.documentTypeCode")}>
            {data.documentTypeCode}
          </InfoItem>
          <InfoItem label={t("fibu.eInvoiceChecker.buyerReference")}>
            {data.buyerReference}
          </InfoItem>
          <InfoItem label={t("fibu.eInvoiceChecker.format")}>
            {data.format}
          </InfoItem>
          <InfoItem label={t("fibu.eInvoiceChecker.profile")}>
            {data.profile}
          </InfoItem>
        </InfoCard>
        <InfoCard title={t("fibu.eInvoiceChecker.amounts")}>
          <InfoItem label={t("fibu.auftrag.nettoSumme._")}>
            {amount(data.totalNetAmount)}
          </InfoItem>
          <InfoItem label={t("fibu.common.vatAmount")}>
            {amount(data.totalTaxAmount)}
          </InfoItem>
          <InfoItem label={t("fibu.rechnung.bruttoBetrag")}>
            {amount(data.totalGrossAmount)}
          </InfoItem>
          <InfoItem label={t("fibu.eInvoiceChecker.amountDue")}>
            {amount(data.amountDue)}
          </InfoItem>
        </InfoCard>
        {data.seller && (
          <PartyCard
            title={t("fibu.eInvoiceChecker.seller")}
            party={data.seller}
            withBank
          />
        )}
        {data.buyer && (
          <PartyCard
            title={t("fibu.eInvoiceChecker.buyer")}
            party={data.buyer}
          />
        )}
      </div>

      {data.lineItems.length > 0 && (
        <SectionCard className="flex flex-col gap-2">
          <h3 className="text-sm font-semibold">
            {t("fibu.eInvoiceChecker.lineItems")}
          </h3>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="text-right">
                  {t("fibu.auftrag.position._")}
                </TableHead>
                <TableHead>{t("description")}</TableHead>
                <TableHead className="text-right">
                  {t("fibu.rechnung.menge")}
                </TableHead>
                <TableHead>{t("fibu.eInvoiceChecker.unit")}</TableHead>
                <TableHead className="text-right">
                  {t("fibu.rechnung.position.einzelNetto")}
                </TableHead>
                <TableHead className="text-right">
                  {t("fibu.auftrag.nettoSumme._")}
                </TableHead>
                <TableHead className="text-right">
                  {t("fibu.rechnung.mehrwertSteuerSatz")}
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.lineItems.map((item) => (
                <TableRow key={item.id || item.position}>
                  <TableCell className="text-right tabular-nums">
                    {item.position}
                  </TableCell>
                  <TableCell className="whitespace-pre-line">
                    {item.description}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {formatNumber(item.quantity, ctx)}
                  </TableCell>
                  <TableCell>{item.unit}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    {amount(item.unitPrice)}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {amount(item.netAmount)}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {item.vatPercent == null
                      ? ""
                      : `${formatNumber(item.vatPercent, ctx)} %`}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </SectionCard>
      )}

      {data.attachments.length > 0 && (
        <SectionCard className="flex flex-col gap-2">
          <h3 className="text-sm font-semibold">
            {t("fibu.eInvoiceChecker.attachments")}
          </h3>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("attachment.fileName")}</TableHead>
                <TableHead>{t("fibu.eInvoiceChecker.mimeType")}</TableHead>
                <TableHead className="text-right">
                  {t("attachment.fileSize")}
                </TableHead>
                <TableHead>{t("description")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.attachments.map((attachment) => (
                <TableRow key={attachment.id}>
                  <TableCell>
                    <Button
                      type="button"
                      variant="link"
                      size="sm"
                      className="h-auto gap-1.5 p-0"
                      onClick={() =>
                        downloadEInvoiceAttachment(attachment.id).catch(
                          (error: Error) => toast.error(error.message)
                        )
                      }
                    >
                      <HugeiconsIcon
                        icon={Download04Icon}
                        size={14}
                        aria-hidden
                      />
                      {attachment.filename}
                    </Button>
                  </TableCell>
                  <TableCell>{attachment.mimeType}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    {attachment.sizeFormatted}
                  </TableCell>
                  <TableCell>{attachment.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </SectionCard>
      )}
    </div>
  );
}

function PartyCard({
  title,
  party,
  withBank,
}: {
  title: string;
  party: EInvoiceParty;
  /** The seller's bank account is where the buyer pays to; the buyer's is not part of the invoice. */
  withBank?: boolean;
}) {
  const t = useTranslations();
  return (
    <InfoCard title={title}>
      <InfoItem label={t("name")}>{party.name}</InfoItem>
      <InfoItem label={t("address.addressText")}>{party.street}</InfoItem>
      <InfoItem label={t("address.zipCode")}>{party.zip}</InfoItem>
      <InfoItem label={t("address.city")}>{party.city}</InfoItem>
      <InfoItem label={t("address.country")}>{party.country}</InfoItem>
      <InfoItem label={t("fibu.konto.vatId")}>{party.vatId}</InfoItem>
      {withBank && (
        <InfoItem label={t("fibu.rechnung.iban")}>{party.iban}</InfoItem>
      )}
    </InfoCard>
  );
}

function InfoCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <SectionCard className="flex flex-col gap-3">
      <h3 className="text-sm font-semibold">{title}</h3>
      <dl className="grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2">
        {children}
      </dl>
    </SectionCard>
  );
}

/** One value with its label above; nothing for an empty value, as the file simply doesn't state it. */
function InfoItem({ label, children }: { label: string; children: ReactNode }) {
  if (children == null || children === "") return null;
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
      <dd className="text-sm break-words">{children}</dd>
    </div>
  );
}
