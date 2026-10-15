"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { SCRIPT_ROUTE } from "@/components/features/script/script-routes";
import { ScriptExecuteView } from "@/components/features/script/script-execute-view";

/** Ad-hoc code of the administration, not stored; `?example=n` starts with that example script. */
function AdHocScriptExecution() {
  const raw = useSearchParams().get("example");
  const example = raw != null && /^\d+$/.test(raw) ? Number(raw) : null;
  return (
    <ScriptExecuteView
      // A new example is a new form: remounting resets what the editor holds.
      key={example ?? "none"}
      endpoint="scriptExecute"
      id={null}
      example={example}
      backRoute={SCRIPT_ROUTE}
    />
  );
}

export default function ScriptExecutePage() {
  return (
    <Suspense>
      <AdHocScriptExecution />
    </Suspense>
  );
}
