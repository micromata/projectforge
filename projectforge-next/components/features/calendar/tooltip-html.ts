/**
 * Turns the HTML tooltip the backend builds (`TooltipBuilder` → `<table><tr><th>Label:</th><td>…</td>`)
 * into structured rows, so the calendar can render it as text instead of injecting markup.
 *
 * The backend escapes most values but deliberately leaves markup in a few (the participant list in
 * `TeamCalEventsProvider`, the task path as `OutputType.HTML` in `TimesheetEventsProvider`, and `<pre>`
 * blocks), so the string genuinely carries tags and cannot simply be rendered as text. Parsing it on a
 * detached `DOMParser` document runs nothing (no scripts, no image requests) and only `textContent`
 * reaches React — the output is provably text. What is lost is styling (anchors, colours), never
 * information.
 */

import type { TooltipRow } from "./types";

export function parseTooltipHtml(html: string): TooltipRow[] {
  const doc = new DOMParser().parseFromString(html, "text/html");
  return [...doc.querySelectorAll("tr")]
    .map((tr) => {
      const td = tr.querySelector("td");
      // A <br> becomes a line break (the participant list uses them); a <pre> keeps its own.
      td?.querySelectorAll("br").forEach((br) => br.replaceWith("\n"));
      const value = td?.textContent?.trim() ?? "";
      return {
        label: (tr.querySelector("th")?.textContent ?? "")
          .replace(/:$/, "")
          .trim(),
        value,
        multiline: !!td?.querySelector("pre") || value.includes("\n"),
      };
    })
    .filter((row) => row.value);
}
