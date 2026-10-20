"use client";

import { lazy, Suspense, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { CopyButton } from "@/components/shared/copy-button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import type { CodeDiffDialogProps } from "./code-diff-dialog";
import { CodeVersionSelect } from "./code-version-select";
import { useCodeVersions } from "./use-code-versions";

const CodeDiffView = lazy(() => import("./code-diff-view"));

/** The content of [CodeDiffDialog]: the two versions' pickers and their diff. */
export function CodeDiffBody({
  original,
  current,
  language,
  versions,
  storedLabel,
  showAll,
}: CodeDiffDialogProps & { showAll: boolean }) {
  const t = useTranslations();
  const v = useCodeVersions({ original, current, source: versions });
  const { leftCode, rightCode } = v;
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-2">
      {/* Which side is which; on a phone the unified diff shows the left as deleted lines. */}
      <div className="grid gap-2 sm:grid-cols-2">
        <SideHeader label={t("codeEditor.diff.left")} code={leftCode}>
          <CodeVersionSelect
            options={v.options}
            value={v.left}
            onChange={v.setLeft}
            storedLabel={storedLabel}
            ariaLabel={t("codeEditor.diff.left")}
          />
        </SideHeader>
        <SideHeader label={t("codeEditor.diff.right")} code={rightCode}>
          <CodeVersionSelect
            options={v.options}
            value={v.right}
            onChange={v.setRight}
            storedLabel={storedLabel}
            ariaLabel={t("codeEditor.diff.right")}
          />
        </SideHeader>
      </div>
      {v.listing || leftCode === undefined || rightCode === undefined ? (
        <Centered>
          <Spinner className="h-5 w-5 border-2" />
        </Centered>
      ) : leftCode === rightCode ? (
        <p className="flex-1 text-sm text-muted-foreground">
          {t("codeEditor.diff.none")}
          {v.noBackups && ` ${t("codeEditor.diff.noBackups")}`}
        </p>
      ) : (
        <Suspense
          fallback={
            <Centered>
              <Spinner className="h-5 w-5 border-2" />
            </Centered>
          }
        >
          <CodeDiffView
            original={leftCode}
            current={rightCode}
            language={language}
            showAll={showAll}
            className="min-h-0 flex-1"
          />
        </Suspense>
      )}
    </div>
  );
}

/**
 * The picker of a side and a button copying that side's code whole — to take a former version over
 * into the editor; also without "show all lines", as the copy is of the version, not of what is shown.
 */
function SideHeader({
  label,
  code,
  children,
}: {
  label: string;
  code: string | undefined;
  children: ReactNode;
}) {
  const t = useTranslations();
  return (
    <div className="flex items-center gap-1">
      {children}
      <HintTooltip title={t("codeEditor.diff.copyAll")}>
        <span>
          <CopyButton value={code} label={label} variant="ghost" />
        </span>
      </HintTooltip>
    </div>
  );
}

function Centered({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-1 items-center justify-center">{children}</div>
  );
}
