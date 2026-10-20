import { RsError } from "@/lib/rs/client";
import type { EntityWriteResult } from "@/lib/rs/entity";
import { toast } from "@/lib/toast";

/**
 * Runs a write of the 2FA setup and reports a failure as a toast.
 *
 * A cancelled 2FA dialog leaves the refused call's 403: nothing to report, the user chose not to.
 *
 * @param onFieldError Gets the validation error of a field (the mobile phone), shown at the field instead.
 * @returns true if the write succeeded.
 */
export async function runSetupWrite(
  write: () => Promise<EntityWriteResult>,
  onFieldError?: (fieldId: string, message: string) => void
): Promise<boolean> {
  try {
    const result = await write();
    if (result.kind === "ok") {
      return true;
    }
    if (result.kind === "rejected") {
      toast.error(result.message);
      return false;
    }
    result.validationErrors.forEach((error) => {
      if (error.fieldId && onFieldError) {
        onFieldError(error.fieldId, error.message);
      } else {
        toast.error(error.message);
      }
    });
    return false;
  } catch (error) {
    if (!(error instanceof RsError && error.status === 403)) {
      toast.error(error instanceof Error ? error.message : String(error));
    }
    return false;
  }
}
