"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { MY_SCRIPT_ROUTE } from "@/components/features/script/script-routes";
import { ScriptExecuteView } from "@/components/features/script/script-execute-view";

/**
 * `/myscript/{id}`: the execution of a script the user is allowed to execute. There is nothing to add
 * here — `new` is only the placeholder of the static export.
 */
export function MyScriptPageClient() {
  const raw = useRouteParams<{ id: string }>("/myscript/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return (
    <ScriptExecuteView
      endpoint="myScriptExecute"
      id={id}
      backRoute={MY_SCRIPT_ROUTE}
    />
  );
}
