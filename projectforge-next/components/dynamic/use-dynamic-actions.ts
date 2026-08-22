"use client";

import { useCallback, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import {
  normalizeAction,
  type NormalizedAction,
} from "@/lib/dynamic/response-action";
import { showResponseMessage } from "@/lib/dynamic/response-toast";
import { resolveMenuUrl, sanitizeRedirectUrl } from "@/lib/menu-url";
import { fetchUserStatus } from "@/lib/rs/client";
import {
  buildPostData,
  callDynamicAction,
  postWatchFields,
  type DynamicMethod,
} from "@/lib/rs/dynamic";
import type { DataObject } from "@/lib/dynamic/path";
import type { ActionDef } from "@/lib/rs/types";
import type { DynamicLayoutStore } from "./use-dynamic-layout";

/** A server that keeps answering with another request would loop forever otherwise. */
const MAX_ACTION_DEPTH = 5;

/**
 * Told that this layout is finished with — for an owner that renders it somewhere the backend's
 * redirect cannot be followed (a dialog, see DynamicFormDialog).
 *
 * @param variables The answer's variables. A save carries the id of the written entry there
 * (`AbstractEntityRest.onAfterEdit`); a cancel carries none, which is how the owner tells the two
 * apart.
 * @param data The form's values at that moment, so the owner needs no second request to name what
 * was saved.
 */
export type DynamicDoneHandler = (
  variables: DataObject,
  data: DataObject
) => void;

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
 *
 * @param onDone Given by an owner that renders the layout instead of a page. Every action that would
 * leave the page then reports back to it rather than navigating — see [DynamicDoneHandler].
 */
export function useDynamicActions(
  store: DynamicLayoutStore,
  category: string,
  queryKey: readonly unknown[],
  onDone?: DynamicDoneHandler
): DynamicActions {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { readState, setIsFetching, setValidationErrors, applyUpdate } = store;
  // Through a ref, so an owner that rebuilds the handler per render does not rebuild every callback
  // below with it — the interpreter is handed to the buttons and lives across a whole edit.
  const doneRef = useRef(onDone);
  useEffect(() => {
    doneRef.current = onDone;
  }, [onDone]);

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

  /** Everything an owned layout ends with: the edit is over, and the owner decides what follows. */
  const reportDone = useCallback(
    (action: NormalizedAction): boolean => {
      const done = doneRef.current;
      if (!done) return false;
      done(action.variables ?? {}, readState().data);
      return true;
    },
    [readState]
  );

  const interpret = useCallback(
    async (action: NormalizedAction, depth: number): Promise<void> => {
      if (action.message) showResponseMessage(action.message);

      switch (action.targetType) {
        case "REDIRECT":
        case "MODAL":
          // A layout the caller owns has no page to leave: the backend answers a save as well as a
          // cancel with a redirect to the list, and for such an owner that means "this edit is
          // over" — which is all a dialog needs to know (see DynamicDoneHandler).
          if (reportDone(action)) break;
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
          // For an owned layout this is literally what it says, and the owner closes it.
          if (reportDone(action)) break;
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
      reportDone,
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
