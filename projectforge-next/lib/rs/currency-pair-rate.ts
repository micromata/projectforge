import { rawRequest, request, RsError } from "./client";
import type { ResponseAction } from "./types";
import type {
  ValidityPrefill,
  ValidityWriteResult,
} from "@/components/shared/validity-entries/types";
import type { CurrencyConversionRate } from "@/components/features/currency-pair/types";

const NOT_ACCEPTABLE = 406;

/** Inserts a conversion rate of a currency pair (`CurrencyPairEntityRest.insertRate`). */
export function insertRate(rate: CurrencyConversionRate) {
  return write("insert", rate);
}

/** Updates a rate; the backend keeps it at the pair it is stored for. */
export function updateRate(rate: CurrencyConversionRate) {
  return write("update", rate);
}

/** Marks the rate as deleted; the backend keeps it for the history. */
export function deleteRate(rate: CurrencyConversionRate) {
  return write("delete", rate);
}

interface OnlineRates {
  conversionRate?: number | null;
  inverseConversionRate?: number | null;
  info?: string | null;
}

/**
 * The rates of both directions for the date, fetched online by the backend (`CurrencyPairEntityRest.getOnlineRates`),
 * as proposal for a new rate. Nothing is stored. `null` if nothing was found.
 */
export async function fetchOnlineRates(
  currencyPairId: number,
  date: string | null
): Promise<ValidityPrefill<CurrencyConversionRate> | null> {
  const params = new URLSearchParams({
    currencyPairId: String(currencyPairId),
  });
  if (date) params.set("date", date);
  const rates = await request<OnlineRates>(
    `/rs/currencyPair/rate/onlineRates?${params.toString()}`,
    { method: "GET" }
  );
  if (rates.conversionRate == null && rates.inverseConversionRate == null) {
    return null;
  }
  return {
    values: {
      conversionRate: rates.conversionRate ?? null,
      inverseConversionRate: rates.inverseConversionRate ?? null,
    },
    info: rates.info ?? null,
  };
}

/**
 * Posts the rate in the `PostData` envelope. Answers with all rates of the pair, the backend's validation errors
 * (406), or its refusal (a danger toast, e.g. no write access).
 */
async function write(
  action: "insert" | "update" | "delete",
  rate: CurrencyConversionRate
): Promise<ValidityWriteResult<CurrencyConversionRate>> {
  const path = `/rs/currencyPair/rate/${action}`;
  const data = {
    id: rate.id ?? null,
    validFrom: rate.validFrom ?? null,
    conversionRate: rate.conversionRate ?? null,
    inverseConversionRate: rate.inverseConversionRate ?? null,
    comment: rate.comment ?? null,
    currencyPairId: rate.currencyPairId,
  };
  const res = await rawRequest(path, {
    method: "POST",
    body: JSON.stringify({ data }),
  });
  if (res.status === NOT_ACCEPTABLE) {
    const body = (await res.json().catch(() => null)) as ResponseAction | null;
    return {
      kind: "validationErrors",
      validationErrors: body?.validationErrors ?? [],
    };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  const body = (await res.json().catch(() => null)) as
    | ({ entries?: CurrencyConversionRate[] } & ResponseAction)
    | null;
  // `GlobalDefaultExceptionHandler` answers a `UserException` (an `AccessException` among them) with HTTP
  // 200 and a danger toast, see lib/rs/entity.ts.
  if (body?.targetType === "TOAST" && body.message?.color === "danger") {
    return {
      kind: "rejected",
      message:
        body.message.message ??
        body.message.technicalMessage ??
        body.message.i18nKey ??
        "",
    };
  }
  return { kind: "ok", entries: body?.entries ?? [] };
}
