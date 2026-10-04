"use client";

import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import type { useAttachmentMutations } from "@/hooks/use-attachments";
import type {
  AttachmentWriteResult,
  EncryptionMode,
} from "@/lib/rs/attachments";

type Mutations = Pick<
  ReturnType<typeof useAttachmentMutations>,
  "encrypt" | "testDecryption"
>;

/**
 * The two password actions of an attachment's details, with their outcome sorted out: a refused
 * password resolves to the backend's message for the password field, everything else is toasted or
 * handed to [onEncrypted].
 *
 * @param onEncrypted called with the answer of an encryption that was not a refused password — the
 * caller reports a refusal and closes the details on success, as the legacy CLOSE_MODAL did: the file
 * they showed is gone, replaced by its ZIP.
 */
export function useAttachmentEncryption(
  { encrypt, testDecryption }: Mutations,
  onEncrypted: (result: AttachmentWriteResult) => void
) {
  const t = useTranslations();

  async function encryptFile(
    fileId: string,
    password: string,
    mode: EncryptionMode
  ): Promise<string | null> {
    try {
      const result = await encrypt.mutateAsync({ fileId, password, mode });
      if (result.kind === "invalid") return result.message;
      onEncrypted(result);
    } catch {
      toast.error(t("validation.error.generic"));
    }
    return null;
  }

  async function testFile(
    fileId: string,
    password: string
  ): Promise<string | null> {
    try {
      const result = await testDecryption.mutateAsync({ fileId, password });
      if (result.kind === "invalid") return result.message;
      if (result.kind === "rejected") {
        toast.error(result.message || t("validation.error.generic"));
      } else {
        toast.success(t("attachment.testDecryption.successful"));
      }
    } catch {
      toast.error(t("validation.error.generic"));
    }
    return null;
  }

  return {
    encryptFile,
    testFile,
    pending: encrypt.isPending || testDecryption.isPending,
  };
}
