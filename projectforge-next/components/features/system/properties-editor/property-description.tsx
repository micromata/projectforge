import { HighlightedText } from "@/components/shared/highlighted-text";
import { cn } from "@/lib/utils";
import { descriptionBlocks } from "./description-blocks";

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
            <HighlightedText text={block.text} query={query} />
          </p>
        ) : (
          <ul key={index} className="list-disc space-y-0.5 pl-4">
            {block.items.map((item, itemIndex) => (
              <li key={itemIndex}>
                <HighlightedText text={item} query={query} />
              </li>
            ))}
          </ul>
        )
      )}
    </div>
  );
}
