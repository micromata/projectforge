"use client";

import {
  useEntityData,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { UserAvatarField } from "@/components/shared/user-avatar-field";
import { cn } from "@/lib/utils";
import type { UserDetail } from "./types";

/**
 * The avatar of an existing user, which an administrator may replace or remove — at once, like the user on
 * the my-account page, not with the save of the form.
 */
export function UserAvatarSection({ className }: { className?: string }) {
  const data = useEntityData<UserDetail>();
  const readOnly = useFormReadOnly();
  if (data?.id == null) return null;
  return (
    <div className={cn(className)}>
      <UserAvatarField
        userId={data.id}
        name={data.displayName}
        avatarVersion={data.avatarVersion}
        readOnly={readOnly}
      />
    </div>
  );
}
