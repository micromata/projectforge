import { Fragment } from "react";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { cn } from "@/lib/utils";
import { descriptionBlocks } from "./description-blocks";
import { inlineSegments } from "./description-inline";

/** A line of a description: text with `code`, **bold** and links (http/https only, opened in a new tab). */
function InlineText({ text, query }: { text: string; query: string }) {
  return (
    <>
      {inlineSegments(text).map((segment, index) => {
        const content = <HighlightedText text={segment.text} query={query} />;
        switch (segment.kind) {
          case "code":
            return (
              <code key={index} className="font-mono">
                {content}
              </code>
            );
          case "bold":
            return (
              <strong key={index} className="font-semibold">
                {content}
              </strong>
            );
          case "link":
            return (
              <a
                key={index}
                href={segment.href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary underline"
              >
                {content}
              </a>
            );
          default:
            return <Fragment key={index}>{content}</Fragment>;
        }
      })}
    </>
  );
}

/** A parameter or block description: small and muted, its paragraphs and bullet lists reflowed. */
export function PropertyDescription({
  text,
  query,
  className,
}: {
  text: string;
  query: string;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex max-w-prose flex-col gap-1 text-xs text-muted-foreground",
        className
      )}
    >
      {descriptionBlocks(text).map((block, index) =>
        block.kind === "paragraph" ? (
          <p key={index}>
            <InlineText text={block.text} query={query} />
          </p>
        ) : (
          <ul key={index} className="list-disc space-y-0.5 pl-4">
            {block.items.map((item, itemIndex) => (
              <li key={itemIndex}>
                <InlineText text={item} query={query} />
              </li>
            ))}
          </ul>
        )
      )}
    </div>
  );
}
