"use client";

import { Suspense, useEffect, useState } from "react";
import { GestureLink } from "@/components/shared/guarded-link";
import { useLocale, useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import {
  fetchSetupState,
  submitSetup,
  type SetupState,
  type SetupPayload,
} from "@/lib/rs/setup";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { AuthCard } from "@/components/shared/auth-card";
import { FormAlert } from "@/components/shared/form-alert";
import { FieldHint } from "@/components/shared/form/field-hint";
import { ValueCombobox } from "@/components/shared/value-combobox";
import { SetupSection } from "@/components/features/setup/setup-section";
import { SetupSubmitButton } from "@/components/features/setup/setup-submit-button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { cn } from "@/lib/utils";

/** The two setup targets, each rendered as a selectable tile. */
const TARGETS = [
  {
    value: "TEST_DATA",
    titleKey: "target.testdata",
    descKey: "target.testdata_description",
  },
  {
    value: "EMPTY_DATABASE",
    titleKey: "target.emptyDatabase",
    descKey: "target.emptyDatabase_description",
  },
] as const;

/** The symbol of a currency in the given locale, e.g. "€" for EUR; the code itself if there is none. */
function currencySymbolOf(code: string, locale: string): string {
  try {
    const part = new Intl.NumberFormat(locale, {
      style: "currency",
      currency: code,
      currencyDisplay: "narrowSymbol",
    })
      .formatToParts(0)
      .find((p) => p.type === "currency");
    return part?.value ?? code;
  } catch {
    return code;
  }
}

/** The name of a currency in the given locale, e.g. "Euro" for EUR; the code itself if there is none. */
function currencyNameOf(code: string, locale: string): string {
  try {
    return new Intl.DisplayNames(locale, { type: "currency" }).of(code) ?? code;
  } catch {
    return code;
  }
}

/** A field label with an optional ⓘ help icon carrying the field's explanation. */
function LabelRow({
  htmlFor,
  label,
  hint,
}: {
  htmlFor: string;
  label: string;
  hint?: string;
}) {
  return (
    <div className="flex items-center gap-1.5">
      <Label htmlFor={htmlFor}>{label}</Label>
      {hint && <FieldHint hint={hint} label={label} />}
    </div>
  );
}

export default function SetupPage() {
  return (
    <Suspense fallback={null}>
      <Setup />
    </Suspense>
  );
}

function Setup() {
  // setup.* — keys owned by this page (moved out of Java bundle)
  const t = useTranslations("setup");
  // shared backend-bundle keys still used across the app
  const tb = useTranslations();
  const locale = useLocale();
  // Spelled out (no template literals): the i18n generator finds literal keys only.
  const localeLabels: Record<string, string> = {
    en: tb("locale.en"),
    de: tb("locale.de"),
  };
  const timeNotationLabels: Record<string, string> = {
    H12: tb("timeNotation.12"),
    H24: tb("timeNotation.24"),
  };

  const [state, setState] = useState<SetupState | null>(null);
  const [loadError, setLoadError] = useState(false);

  const [target, setTarget] = useState<"EMPTY_DATABASE" | "TEST_DATA">(
    "TEST_DATA"
  );
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [passwordRepeat, setPasswordRepeat] = useState("");
  const [timeZone, setTimeZone] = useState("");
  const [calendarDomain, setCalendarDomain] = useState("");
  const [sysopEMail, setSysopEMail] = useState("");
  const [feedbackEMail, setFeedbackEMail] = useState("");
  const [defaultLocale, setDefaultLocale] = useState("");
  const [defaultTimeNotation, setDefaultTimeNotation] = useState("");
  const [defaultFirstDayOfWeek, setDefaultFirstDayOfWeek] = useState("");
  const [currency, setCurrency] = useState("");
  const [currencySymbol, setCurrencySymbol] = useState("");
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  const [fieldError, setFieldError] = useState<{
    field?: string | null;
    message: string;
  } | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchSetupState()
      .then((s) => {
        if (cancelled) return;
        setState(s);
        if (!s.alreadyInitialized) {
          setUsername(s.defaultUsername);
          setCalendarDomain(s.defaultCalendarDomain);
          setTimeZone(s.defaultTimeZone);
          setDefaultLocale(s.regional.defaultLocale ?? "");
          setDefaultTimeNotation(s.regional.defaultTimeNotation ?? "");
          setDefaultFirstDayOfWeek(s.regional.defaultFirstDayOfWeek ?? "");
          setCurrency(s.regional.currency ?? "");
          setCurrencySymbol(s.regional.currencySymbol ?? "");
        }
      })
      .catch(() => {
        if (!cancelled) setLoadError(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (loadError) {
    return (
      <AuthCard title={t("title")}>
        <FormAlert tone="error">{t("loadError")}</FormAlert>
      </AuthCard>
    );
  }

  if (!state) {
    return (
      <AuthCard title={t("title")}>
        <div className="flex justify-center py-6">
          <div className="size-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
        </div>
      </AuthCard>
    );
  }

  if (state.alreadyInitialized) {
    return (
      <AuthCard title={t("title")}>
        <div className="grid gap-4">
          <FormAlert tone="info">{t("alreadyInitialized")}</FormAlert>
          <GestureLink
            href="/login"
            className="text-center text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
          >
            {t("backToLogin")}
          </GestureLink>
        </div>
      </AuthCard>
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFieldError(null);
    setIsSubmitting(true);
    try {
      const payload: SetupPayload = {
        setupTarget: target,
        username,
        password,
        passwordRepeat,
        timeZone,
        calendarDomain,
        sysopEMail: sysopEMail || undefined,
        feedbackEMail: feedbackEMail || undefined,
        defaultLocale,
        defaultTimeNotation,
        defaultFirstDayOfWeek,
        currency,
        currencySymbol,
      };
      const result = await submitSetup(payload);
      if (result.success) {
        // No auto-login: a full page load to the login page starts the app from a clean session
        // (the fresh database changes caches, menus and user state).
        const resolved = resolveMenuUrl(result.redirectUrl ?? "/login");
        window.location.assign(toAbsoluteUrl(resolved));
      } else {
        if (result.field === "sysopEMail" || result.field === "feedbackEMail") {
          setNotificationsOpen(true);
        }
        setFieldError({
          field: result.field,
          message: result.message ?? t("unknownError"),
        });
      }
    } catch {
      setFieldError({ message: t("unknownError") });
    } finally {
      setIsSubmitting(false);
    }
  }

  const currencyOptions = state.availableCurrencies.map((code) => ({
    value: code,
    label: `${code} – ${currencyNameOf(code, locale)} (${currencySymbolOf(code, locale)})`,
  }));

  return (
    <AuthCard title={t("title")} className="max-w-3xl">
      <form onSubmit={handleSubmit} noValidate className="grid gap-4">
        {fieldError && !fieldError.field && (
          <FormAlert tone="error">{fieldError.message}</FormAlert>
        )}

        {/* Setup target — selectable tiles */}
        <div className="grid gap-2">
          <Label>{t("target._")}</Label>
          <RadioGroup
            value={target}
            onValueChange={(v) =>
              setTarget(v as "EMPTY_DATABASE" | "TEST_DATA")
            }
            className="grid gap-3 sm:grid-cols-2"
          >
            {TARGETS.map((item) => {
              const selected = target === item.value;
              const itemId = `target-${item.value}`;
              return (
                <Label
                  key={item.value}
                  htmlFor={itemId}
                  className={cn(
                    "flex cursor-pointer flex-col gap-2 rounded-lg border p-4 transition-colors",
                    selected
                      ? "border-primary bg-primary/5 ring-1 ring-primary"
                      : "border-input hover:bg-accent"
                  )}
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value={item.value} id={itemId} />
                    <span className="text-sm font-medium">
                      {t(item.titleKey)}
                    </span>
                  </div>
                  <span className="text-sm font-normal leading-snug text-muted-foreground">
                    {t(item.descKey)}
                  </span>
                </Label>
              );
            })}
          </RadioGroup>
        </div>

        {/* Administrator account */}
        <SetupSection
          title={t("section.account._")}
          description={t("section.account.description")}
        >
          <div className="grid gap-2 sm:col-span-2">
            <Label htmlFor="username">{tb("username")}</Label>
            <Input
              id="username"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="password">{tb("password._")}</Label>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {fieldError?.field === "password" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="passwordRepeat">{tb("passwordRepeat")}</Label>
            <Input
              id="passwordRepeat"
              type="password"
              autoComplete="new-password"
              required
              value={passwordRepeat}
              onChange={(e) => setPasswordRepeat(e.target.value)}
            />
            {fieldError?.field === "passwordRepeat" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
        </SetupSection>

        {/* Time and region: stored in projectforge.properties / the configuration, effective at once. */}
        <SetupSection
          title={t("section.region._")}
          description={t("section.region.description")}
        >
          <div className="grid gap-2">
            <LabelRow
              htmlFor="timeZone"
              label={tb("administration.configuration.param.timezone._")}
              hint={tb(
                "administration.configuration.param.timezone.description"
              )}
            />
            <ValueCombobox
              id="timeZone"
              options={state.availableTimeZones.map((tz) => ({
                value: tz.id,
                label: tz.id,
              }))}
              selected={timeZone ? [timeZone] : []}
              onChange={(vals) => {
                if (vals[0]) setTimeZone(vals[0]);
              }}
              aria-label={tb("administration.configuration.param.timezone._")}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="defaultLocale">{t("regional.locale")}</Label>
            <ValueCombobox
              id="defaultLocale"
              options={state.availableLocales.map((l) => ({
                value: l,
                label: localeLabels[l] ?? l,
              }))}
              selected={defaultLocale ? [defaultLocale] : []}
              onChange={(vals) => {
                if (vals[0]) setDefaultLocale(vals[0]);
              }}
              aria-label={t("regional.locale")}
            />
            {fieldError?.field === "defaultLocale" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="defaultTimeNotation">{tb("timeNotation._")}</Label>
            <ValueCombobox
              id="defaultTimeNotation"
              options={state.availableTimeNotations.map((n) => ({
                value: n,
                label: timeNotationLabels[n] ?? n,
              }))}
              selected={defaultTimeNotation ? [defaultTimeNotation] : []}
              onChange={(vals) => {
                if (vals[0]) setDefaultTimeNotation(vals[0]);
              }}
              aria-label={tb("timeNotation._")}
            />
            {fieldError?.field === "defaultTimeNotation" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="defaultFirstDayOfWeek">
              {tb("calendar.firstDayOfWeek")}
            </Label>
            <ValueCombobox
              id="defaultFirstDayOfWeek"
              options={state.availableFirstDaysOfWeek.map((d, index) => ({
                value: d,
                // 2024-01-01 is a Monday, DayOfWeek names start with MONDAY.
                label: new Intl.DateTimeFormat(locale, {
                  weekday: "long",
                }).format(new Date(2024, 0, 1 + index)),
              }))}
              selected={defaultFirstDayOfWeek ? [defaultFirstDayOfWeek] : []}
              onChange={(vals) => {
                if (vals[0]) setDefaultFirstDayOfWeek(vals[0]);
              }}
              aria-label={tb("calendar.firstDayOfWeek")}
            />
            {fieldError?.field === "defaultFirstDayOfWeek" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="currency">{t("regional.currency")}</Label>
            <ValueCombobox
              id="currency"
              options={currencyOptions}
              selected={currency ? [currency] : []}
              onChange={(vals) => {
                if (!vals[0]) return;
                setCurrency(vals[0]);
                setCurrencySymbol(currencySymbolOf(vals[0], locale));
              }}
              aria-label={t("regional.currency")}
            />
            {fieldError?.field === "currency" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
          <div className="grid gap-2">
            <LabelRow
              htmlFor="currencySymbol"
              label={t("regional.currencySymbol")}
              hint={t("regional.currencySymbolHint")}
            />
            <Input
              id="currencySymbol"
              autoCorrect="off"
              spellCheck={false}
              maxLength={5}
              value={currencySymbol}
              onChange={(e) => setCurrencySymbol(e.target.value)}
            />
            {fieldError?.field === "currencySymbol" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
          <div className="grid gap-2">
            <LabelRow
              htmlFor="calendarDomain"
              label={tb("administration.configuration.param.calendarDomain._")}
              hint={tb(
                "administration.configuration.param.calendarDomain.description"
              )}
            />
            <Input
              id="calendarDomain"
              required
              value={calendarDomain}
              onChange={(e) => setCalendarDomain(e.target.value)}
            />
            {fieldError?.field === "calendarDomain" && (
              <p className="text-sm text-destructive">{fieldError.message}</p>
            )}
          </div>
        </SetupSection>

        {/* E-mail addresses: optional, so folded away. */}
        <Collapsible
          open={notificationsOpen}
          onOpenChange={setNotificationsOpen}
          className="rounded-lg border"
        >
          <CollapsibleTrigger className="flex w-full items-center justify-between gap-2 p-4 text-left">
            <span className="grid gap-0.5">
              <span className="text-sm font-medium">
                {t("section.notifications._")}
              </span>
              <span className="text-sm text-muted-foreground">
                {t("section.notifications.description")}
              </span>
            </span>
            <HugeiconsIcon
              icon={ArrowDown01Icon}
              className={cn(
                "size-4 shrink-0 transition-transform",
                notificationsOpen && "rotate-180"
              )}
            />
          </CollapsibleTrigger>
          <CollapsibleContent className="grid gap-4 px-4 pb-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <LabelRow
                htmlFor="sysopEMail"
                label={t("sysopEMail")}
                hint={tb(
                  "administration.configuration.param.systemAdministratorEMail.description"
                )}
              />
              <Input
                id="sysopEMail"
                type="email"
                value={sysopEMail}
                onChange={(e) => setSysopEMail(e.target.value)}
              />
            </div>
            <div className="grid gap-2">
              <LabelRow
                htmlFor="feedbackEMail"
                label={t("feedbackEMail")}
                hint={tb(
                  "administration.configuration.param.feedbackEMail.description"
                )}
              />
              <Input
                id="feedbackEMail"
                type="email"
                value={feedbackEMail}
                onChange={(e) => setFeedbackEMail(e.target.value)}
              />
            </div>
          </CollapsibleContent>
        </Collapsible>

        <SetupSubmitButton
          isSubmitting={isSubmitting}
          label={t("finish")}
          submittingLabel={t("submitting")}
        />
      </form>
    </AuthCard>
  );
}
