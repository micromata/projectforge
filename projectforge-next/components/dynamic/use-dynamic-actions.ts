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
import type { ActionDef, MagicFilter } from "@/lib/rs/types";
import type { DynamicLayoutStore } from "./use-dynamic-layout";

/** A server that keeps answering with another request would loop forever otherwise. */
const MAX_ACTION_DEPTH = 5;

/** Sub paths of the two list actions every list layout carries (RestPaths.LIST / FILTER_RESET). */
const LIST_PATH = "list";
const FILTER_RESET_PATH = "filterReset";

/** What a list page whose response carried no filter searches with — everything, unsorted. */
const EMPTY_FILTER: MagicFilter = { entries: [], sortProperties: [] };

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
      const { data, serverData, filter } = readState();
      // The search button of a list page. Its request is the one that is not about a form: the
      // endpoint takes the bare `MagicFilter` (AbstractPagesRest.getList), and the `PostData` wrapper
      // every other action sends made Spring reject it with `Unrecognized field "data"`. The legacy
      // app posts the filter here as well (see actions/list/index.js).
      const isListRequest = url === `${category}/${LIST_PATH}`;
      // The reset button beside it. It answers with the filter it fell back to, which is not a
      // `ResponseAction` either — but the backend has stored that filter in the user's prefs, so
      // refetching the page shows the reset list without this hook having to seed a filter itself.
      const isFilterReset = url === `${category}/${FILTER_RESET_PATH}`;
      setIsFetching(true);
      setValidationErrors([]);
      try {
        const result = await callDynamicAction(
          method,
          url,
          isListRequest
            ? (filter ?? EMPTY_FILTER)
            : buildPostData(data, serverData, triggered)
        );
        if (result.kind === "validationErrors") {
          setValidationErrors(result.validationErrors);
          window.scrollTo(0, 0);
          return;
        }
        if (result.kind === "download") return;
        // …and its answer is not an action either, but the `ResultSet` itself — which is what the
        // page renders its rows from (`data.resultSet`, see DynamicGrid), so it replaces the data.
        if (isListRequest) {
          applyUpdate({ data: result.response as unknown as DataObject });
          return;
        }
        if (isFilterReset) {
          await queryClient.invalidateQueries({ queryKey });
          return;
        }
        // The answer is itself an action, so keep interpreting: that is what turns a save into a
        // redirect and a delete into a list reload.
        await interpretRef.current(normalizeAction(result.response), depth + 1);
      } catch (err) {
        toast.error(err instanceof Error ? err.message : "Action failed");
      } finally {
        setIsFetching(false);
      }
    },
    [
      applyUpdate,
      category,
      queryClient,
      queryKey,
      readState,
      setIsFetching,
      setValidationErrors,
    ]
  );

  const interpret = useCallback(
    async (action: NormalizedAction, depth: number): Promise<void> => {
      if (action.message) showResponseMessage(action.message);

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
