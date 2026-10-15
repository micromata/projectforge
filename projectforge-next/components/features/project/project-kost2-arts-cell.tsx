"use client";

import { Fragment } from "react";
import { useTranslations } from "next-intl";
import type { Kost2ArtSelection } from "./types";

/**
 * The cost 2 types column of the project list: the two-digit numbers of the project's cost 2 units, a
 * non-active (or ended) one struck through as any deactivated reference is (see useDeclaredColumns). The
 * tooltip lists them one per line with their name (no description, so each stays one line), marking the
 * non-active ones.
 *
 * The tooltip is set here, not by the column's `tooltip`, because it needs a translation; the table's one
 * delegated tooltip finds the `data-tooltip` by `closest` (see useOverflowTooltip).
 */
export function ProjectKost2ArtsCell({
  arts,
}: {
  arts: Kost2ArtSelection[] | null | undefined;
}) {
  const t = useTranslations();
  if (!arts?.length) return null;
  const number = (art: Kost2ArtSelection) => String(art.id).padStart(2, "0");
  const tooltip = arts
    .map((art) => {
      const line = `${number(art)} ${art.name ?? ""}`.trim();
      return art.active ? line : `${line} (${t("fibu.kost.status.nonactive")})`;
    })
    .join("\n");
  return (
    <span
      className="block truncate font-mono text-muted-foreground"
      data-tooltip={tooltip}
    >
      {arts.map((art, index) => (
        <Fragment key={art.id}>
          {index > 0 && ", "}
          {art.active ? (
            number(art)
          ) : (
            <span className="line-through decoration-destructive decoration-2">
              {number(art)}
            </span>
          )}
        </Fragment>
      ))}
    </span>
  );
}
