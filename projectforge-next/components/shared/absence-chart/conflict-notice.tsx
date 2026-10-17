"use client";

import { Alert, AlertDescription } from "@/components/ui/alert";

/**
 * Above an entity form: the warning of a day without any substitute on duty, as it follows the form (e.g. a vacation's
 * or an availability entry's stats). Nothing without a conflict.
 */
export function ConflictNotice({
  conflict,
  message,
}: {
  conflict: boolean | null | undefined;
  message: string;
}) {
  if (conflict !== true) return null;
  return (
    <Alert variant="destructive" className="mx-6 mt-4 w-auto">
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  );
}
