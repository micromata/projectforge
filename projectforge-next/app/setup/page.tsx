"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
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
import { Button } from "@/components/ui/button";
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
          <Link
            href="/login"
            className="text-center text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
          >
            {t("backToLogin")}
          </Link>
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
      };
      const result = await submitSetup(payload);
      if (result.success) {
        // No auto-login: a full page load to the login page starts the app from a clean session
        // (the fresh database changes caches, menus and user state).
        const resolved = resolveMenuUrl(result.redirectUrl ?? "/login");
        window.location.assign(toAbsoluteUrl(resolved));
      } else {
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

  return (
    <AuthCard title={t("title")} className="max-w-2xl">
      <form onSubmit={handleSubmit} className="grid gap-4">
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

        {/* Username */}
        <div className="grid gap-2">
          <Label htmlFor="username">{tb("username")}</Label>
          <Input
            id="username"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>

        {/* Password */}
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

        {/* Password repeat */}
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

        {/* Timezone */}
        <div className="grid gap-2">
          <LabelRow
            htmlFor="timeZone"
            label={tb("administration.configuration.param.timezone._")}
            hint={tb("administration.configuration.param.timezone.description")}
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

        {/* Calendar domain */}
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

        {/* Sysop e-mail (optional) */}
        <div className="grid gap-2">
          <LabelRow
            htmlFor="sysopEMail"
            label={tb(
              "administration.configuration.param.systemAdministratorEMail.label"
            )}
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

        {/* Feedback e-mail (optional) */}
        <div className="grid gap-2">
          <LabelRow
            htmlFor="feedbackEMail"
            label={tb("administration.configuration.param.feedbackEMail.label")}
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

        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? t("submitting") : t("finish")}
        </Button>
      </form>
    </AuthCard>
  );
}
