"use client";

import { useCallback, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  normalizeAction,
  type NormalizedAction,
} from "@/lib/dynamic/response-action";
import { resolveMenuUrl, sanitizeRedirectUrl } from "@/lib/menu-url";
import { fetchUserStatus } from "@/lib/rs/client";
import {
  buildPostData,
  callDynamicAction,
  postWatchFields,
  type DynamicMethod,
} from "@/lib/rs/dynamic";
import type { ActionDef, ResponseActionMessage } from "@/lib/rs/types";
import type { DynamicLayoutStore } from "./use-dynamic-layout";

/** A server that keeps answering with another request would loop forever otherwise. */
const MAX_ACTION_DEPTH = 5;

export interface DynamicActions {
  callAction: (action: ActionDef) => Promise<void>;
  triggerWatchFields: (triggered: string[]) => void;
}

/**
 * Interprets the backend's `ResponseAction`s - the heart of the UILayout protocol.
 *
 * Every button of a dynamic page carries its own url, http method and follow-up instruction, so
 * this hook is what makes save/cancel/delete/clone work without the frontend knowing a single
 * endpoint. Mirrors `callAction` in projectforge-webapp/src/actions/form.js, the reference for the
 * semantics of each target type.
 */
export function useDynamicActions(
  store: DynamicLayoutStore,
  category: string,
  queryKey: readonly unknown[]
): DynamicActions {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { readState, setIsFetching, setValidationErrors, applyUpdate } = store;

  const navigate = useCallback(
    (url: string | undefined) => {
      const safe = sanitizeRedirectUrl(url);
      if (!safe) return;
      const target = resolveMenuUrl(safe);
      if (target.kind === "internal") {
        // A route of this app: next/navigation prepends the base path itself.
        router.push(target.href);
      } else {
        // The legacy React app or Wicket - only a full page load reaches those.
        window.location.assign(target.href);
      }
    },
    [router]
  );

  /**
   * Interpreting and requesting call each other (a save answers with a redirect, a redirect may
   * answer with another request), so the recursion goes through a ref instead of a dependency
   * cycle between the two callbacks. Both are only ever called from event handlers.
   */
  const interpretRef = useRef<
    (action: NormalizedAction, depth: number) => Promise<void>
  >(async () => {});

  const sendRequest = useCallback(
    async (
      method: DynamicMethod,
      url: string,
      depth: number,
      triggered?: string[]
    ): Promise<void> => {
      const { data, serverData } = readState();
      setIsFetching(true);
      setValidationErrors([]);
      try {
        const result = await callDynamicAction(
          method,
          url,
          buildPostData(data, serverData, triggered)
        );
        if (result.kind === "validationErrors") {
          setValidationErrors(result.validationErrors);
          window.scrollTo(0, 0);
          return;
        }
        if (result.kind === "download") return;
        // The answer is itself an action, so keep interpreting: that is what turns a save into a
        // redirect and a delete into a list reload.
        await interpretRef.current(normalizeAction(result.response), depth + 1);
      } catch (err) {
        toast.error(err instanceof Error ? err.message : "Action failed");
      } finally {
        setIsFetching(false);
      }
    },
    [readState, setIsFetching, setValidationErrors]
  );

  const interpret = useCallback(
    async (action: NormalizedAction, depth: number): Promise<void> => {
      if (action.message) showMessage(action.message);

      switch (action.targetType) {
        case "REDIRECT":
        case "MODAL":
          // MODAL has no equivalent in the App Router yet (the legacy app stacked it onto the
          // router's location state), so the page is opened instead of overlaid.
          navigate(action.url);
          break;
        case "UPDATE":
          applyUpdate(action.variables ?? {}, action.merge);
          setValidationErrors(action.validationErrors ?? []);
          if (action.url) navigate(action.url);
          window.scrollTo(0, 0);
          break;
        case "CLOSE_MODAL":
          // Without a modal stack, going back to the previous page is the closest equivalent.
          router.back();
          break;
        case "RELOAD":
          await queryClient.invalidateQueries({ queryKey });
          break;
        case "CHECK_AUTHENTICATION":
          await fetchUserStatus();
          navigate(action.url);
          break;
        case "DOWNLOAD":
          // The browser fetches a download url on its own; the response never reaches us.
          if (action.url) window.open(action.url, "_blank");
          break;
        case "GET":
        case "POST":
        case "PUT":
        case "DELETE": {
          if (!action.url) break;
          if (depth >= MAX_ACTION_DEPTH) {
            toast.error(`Too many chained actions (${action.url}).`);
            break;
          }
          await sendRequest(action.targetType, action.url, depth);
          break;
        }
        case "NOTHING":
        case undefined:
          break;
        default:
          toast.error(`TargetType ${action.targetType} not implemented.`);
      }
    },
    [
      applyUpdate,
      navigate,
      queryClient,
      queryKey,
      router,
      sendRequest,
      setValidationErrors,
    ]
  );
  useEffect(() => {
    interpretRef.current = interpret;
  }, [interpret]);

  const callAction = useCallback(
    async (action: ActionDef) => {
      // The url and the method live in the button's responseAction, never on the button itself.
      if (!action.responseAction) return;
      if (action.confirmMessage && !window.confirm(action.confirmMessage)) {
        return;
      }
      await interpret(action.responseAction, 0);
    },
    [interpret]
  );

  const triggerWatchFields = useCallback(
    (triggered: string[]) => {
      const { data, serverData } = readState();
      void postWatchFields(category, buildPostData(data, serverData, triggered))
        .then((result) => {
          if (result.kind !== "action") return;
          return interpret(normalizeAction(result.response), 0);
        })
        .catch(() => {
          // A failed watch field update must not interrupt typing; the form stays usable.
        });
    },
    [category, interpret, readState]
  );

  return { callAction, triggerWatchFields };
}

function showMessage(message: ResponseActionMessage): void {
  // Already translated by the backend (ResponseAction.Message resolves its i18nKey in the init
  // block), so there is nothing left to look up here.
  const text = message.message ?? message.technicalMessage ?? message.i18nKey;
  if (!text) return;
  if (message.color === "danger") toast.error(text);
  else if (message.color === "warning") toast.warning(text);
  else if (message.color === "success") toast.success(text);
  else toast.info(text);
}
