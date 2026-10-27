"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { Kost2ReallocationWizard } from "@/components/features/kost2-reallocation/kost2-reallocation-wizard";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function Kost2ReallocationRunPageClient() {
  const raw = useRouteParams<{ id: string }>("/kost2-reallocation/[id]")?.id;
  if (raw === undefined) return null;
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isInteger(id) || id <= 0)) notFound();
  return <Kost2ReallocationWizard runId={isNew ? null : id} />;
}
