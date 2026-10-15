/**
 * The calls the to-do form needs beyond the generic entity ones: the user's templates
 * (`ToDoEntityRest`, served under `todo/templates`). The close action runs through the form's own
 * submit (`actions: ["close"]` of TODO_PAGE), reads and writes of the entity through the generic ones.
 */

import { request } from "./client";
import type { ToDoDetail } from "@/components/features/todo/types";
import type { FavoriteIdTitle } from "./types";

const TEMPLATES = "/rs/todo/templates";

/** The list every one of the writes below answers with, so the menu never refetches after one. */
interface TemplatesResult {
  templates?: FavoriteIdTitle[];
}

export async function fetchToDoTemplates(
  signal?: AbortSignal
): Promise<FavoriteIdTitle[]> {
  const result = await request<TemplatesResult>(
    `${TEMPLATES}/list`,
    { method: "GET" },
    signal
  );
  return result.templates ?? [];
}

/**
 * The to-do a template stands for, its references with their names. A value the template doesn't hold
 * is absent, so the form keeps its own there (see templateFieldsOf).
 */
export function selectToDoTemplate(
  id: number,
  signal?: AbortSignal
): Promise<ToDoDetail> {
  return request<ToDoDetail>(
    `${TEMPLATES}/select?id=${id}`,
    { method: "GET" },
    signal
  );
}

/** Saves the to-do on screen as a template under `name`, and answers with the new list. */
export async function createToDoTemplate(
  name: string,
  todo: ToDoDetail,
  signal?: AbortSignal
): Promise<FavoriteIdTitle[]> {
  const result = await request<TemplatesResult>(
    `${TEMPLATES}/create`,
    { method: "POST", body: JSON.stringify({ name, todo }) },
    signal
  );
  return result.templates ?? [];
}

export async function deleteToDoTemplate(
  id: number,
  signal?: AbortSignal
): Promise<FavoriteIdTitle[]> {
  const result = await request<TemplatesResult>(
    `${TEMPLATES}/delete?id=${id}`,
    { method: "GET" },
    signal
  );
  return result.templates ?? [];
}

export async function renameToDoTemplate(
  id: number,
  newName: string,
  signal?: AbortSignal
): Promise<FavoriteIdTitle[]> {
  const result = await request<TemplatesResult>(
    `${TEMPLATES}/rename?id=${id}&newName=${encodeURIComponent(newName)}`,
    { method: "GET" },
    signal
  );
  return result.templates ?? [];
}
