"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { CropIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  ImageEditorDialog,
  type EditableImage,
} from "@/components/shared/image-editor/image-editor-dialog";
import { UserAvatar } from "@/components/shared/user-avatar";
import { Button } from "@/components/ui/button";
import {
  deleteUserAvatar,
  uploadUserAvatar,
  userAvatarUrl,
} from "@/lib/rs/user-avatar";
import { toast } from "@/lib/toast";

const ACCEPTED_IMAGES = "image/png,image/jpeg,image/gif";

/**
 * The width an avatar is scaled down to at most: plenty for the large view here, the chat and the menu show the
 * small preview the backend makes of it (`UserAvatarDao.PREVIEW_SIZE`).
 */
const AVATAR_MAX_WIDTH = 400;

/**
 * The avatar of a user (my account, user edit page): shown, replaced by an upload or removed — at once, not with
 * the save of the surrounding form, so it doesn't make the form dirty. A chosen file and the shown avatar are cut
 * square and scaled down in the image editor first; only its result is uploaded. The user menu and the chat
 * pick the change up through the refetched user status and their next fetch.
 */
export function UserAvatarField({
  userId,
  name,
  avatarVersion,
  readOnly = false,
}: {
  userId: number;
  /** For the initials shown without an avatar. */
  name: string | null | undefined;
  avatarVersion: number | null | undefined;
  readOnly?: boolean;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  // The version after a change here; until then the one loaded with the page.
  const [changedVersion, setChangedVersion] = useState<{
    value: number | null;
  } | null>(null);
  const version = changedVersion
    ? changedVersion.value
    : (avatarVersion ?? null);
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState<EditableImage | null>(null);
  const [confirmRemove, setConfirmRemove] = useState(false);

  const changed = (newVersion: number | null) => {
    setChangedVersion({ value: newVersion });
    void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
  };

  // A failure is thrown on to the editor, which shows it and stays open with its settings.
  const upload = async (file: File) => {
    setBusy(true);
    try {
      changed(await uploadUserAvatar(userId, file));
      toast.success(t("user.avatar.saved"));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    setConfirmRemove(false);
    setBusy(true);
    try {
      await deleteUserAvatar(userId);
      changed(null);
      toast.success(t("user.avatar.removed"));
    } catch (e) {
      toast.error(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  // The object url of a chosen file is released when the editor closes, also by leaving the page.
  const editingSrc = editing?.src;
  useEffect(
    () => () => {
      if (editingSrc?.startsWith("blob:")) URL.revokeObjectURL(editingSrc);
    },
    [editingSrc]
  );

  // `accept` only filters the file chooser: a file dropped in may be anything.
  const edit = (file: File) => {
    if (!ACCEPTED_IMAGES.split(",").includes(file.type)) {
      toast.error(
        t("file.upload.error.unsupportedFormat", { arg0: "PNG, JPEG, GIF" })
      );
      return;
    }
    setEditing({
      src: URL.createObjectURL(file),
      type: file.type,
      name: file.name,
    });
  };

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-start">
      <div className="flex flex-col items-start gap-2">
        <UserAvatar
          name={name}
          userId={userId}
          avatarVersion={version}
          full
          className="size-32 text-3xl"
        />
        {!readOnly && version != null && (
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={busy}
              onClick={() =>
                setEditing({
                  src: userAvatarUrl(userId, version),
                  name: "avatar",
                })
              }
            >
              <HugeiconsIcon icon={CropIcon} />
              {t("user.avatar.edit")}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={busy}
              onClick={() => setConfirmRemove(true)}
            >
              <HugeiconsIcon icon={Delete02Icon} />
              {t("user.avatar.remove._")}
            </Button>
          </div>
        )}
      </div>
      {!readOnly && (
        <div className="flex flex-1 flex-col gap-2">
          <AttachmentDropArea
            onFiles={([file]) => file && edit(file)}
            accept={ACCEPTED_IMAGES}
            multiple={false}
            disabled={busy}
            label={t("user.avatar.upload")}
          />
          <p className="text-xs text-muted-foreground">
            {t("user.avatar.hint")}
          </p>
        </div>
      )}
      {editing && (
        <ImageEditorDialog
          image={editing}
          aspect={1}
          maxWidth={AVATAR_MAX_WIDTH}
          round
          onApply={upload}
          onClose={() => setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={confirmRemove}
        onOpenChange={setConfirmRemove}
        title={t("user.avatar._")}
        description={t("user.avatar.remove.confirm")}
        confirmLabel={t("user.avatar.remove._")}
        destructive
        onConfirm={() => void remove()}
      />
    </div>
  );
}
