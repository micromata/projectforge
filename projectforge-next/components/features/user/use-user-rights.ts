"use client";

import { useEffect } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import {
  fetchUserRights,
  type UserRightInfo,
  type UserRightValue,
} from "@/lib/rs/user";
import type { UserValues } from "./user-schema";

/**
 * The configurable rights of the user in the form, as `UserEntityRest.rights` describes them for the
 * groups the form currently assigns — asked again whenever they change, which replaces the watch field
 * re-layout of the legacy page.
 *
 * The form's `userRights` follow the answer: a right the new groups no longer offer is dropped, a new
 * one added with its stored value, and a value not saved yet survives (it goes with the request). The
 * values are only written where they differ, so loading the page leaves the form untouched — the backend
 * starts the DTO with the same set (`UserEntityRest.transformFromDB`).
 */
export function useUserRights(): UserRightInfo[] {
  const form = useEntityEditForm();
  // Undefined until the entity (or the preset of a new one) has loaded: the form holds its empty
  // defaults until then, whose rights nobody asked for.
  const loaded = useEntityData() !== undefined;
  const id = useStore(form.store, (s: unknown) => (s as FormState).values.id);
  const groupIds = useStore(form.store, (s: unknown) =>
    (s as FormState).values.assignedGroups
      .map((group) => group.id)
      .sort((a, b) => a - b)
      .join(",")
  );
  const { data } = useQuery({
    queryKey: ["user", "rights", id, groupIds],
    queryFn: ({ signal }) => {
      const values = (form.state as FormState).values;
      return fetchUserRights(
        {
          userId: id,
          assignedGroups: values.assignedGroups.map((group) => ({
            id: group.id,
          })),
          userRights: values.userRights,
        },
        signal
      );
    },
    enabled: loaded,
    // The rights of the previous groups stay on screen until the new ones arrive, rather than flicker.
    placeholderData: keepPreviousData,
  });

  useEffect(() => {
    if (!data) return;
    const current = (form.state as FormState).values.userRights;
    const next = mergeRights(data, current);
    if (!sameRights(current, next)) {
      form.setFieldValue("userRights" as never, next as never);
    }
  }, [data, form]);

  return data ?? [];
}

/**
 * The form's `userRights` for the answer [infos]. The answer may come from the cache of earlier groups
 * (going back to them), holding the values of back then: what the form holds now ([current]) wins, as it
 * does in the backend's answer.
 */
export function mergeRights(
  infos: UserRightInfo[],
  current: UserRightValue[]
): UserRightValue[] {
  return infos.map((info) =>
    toValue(
      info,
      current.find((right) => right.rightId === info.rightId)
    )
  );
}

/**
 * The form's value of [info]: the one it [held] already, else the answered one. A select value the
 * right no longer offers is dropped, as the backend drops it.
 */
function toValue(info: UserRightInfo, held?: UserRightValue): UserRightValue {
  if (info.booleanType) {
    return {
      rightId: info.rightId,
      value: null,
      booleanValue: (held ? held.booleanValue : info.booleanValue) ?? null,
    };
  }
  const value = held ? held.value : info.value;
  return {
    rightId: info.rightId,
    value: info.values.some((option) => option.value === value)
      ? (value ?? null)
      : null,
    booleanValue: null,
  };
}

/** Same rights in the same order with the same values — a boolean right by its box, any other by its value. */
function sameRights(
  current: UserRightValue[],
  next: UserRightValue[]
): boolean {
  return (
    current.length === next.length &&
    current.every((right, index) => {
      const other = next[index];
      if (right.rightId !== other.rightId) return false;
      return other.booleanValue != null || right.booleanValue != null
        ? (right.booleanValue ?? null) === (other.booleanValue ?? null)
        : (right.value ?? null) === (other.value ?? null);
    })
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<UserValues, "id" | "assignedGroups" | "userRights">;
}
