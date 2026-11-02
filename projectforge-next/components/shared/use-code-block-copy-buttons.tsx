"use client";

import {
  useLayoutEffect,
  useState,
  type ReactNode,
  type RefObject,
} from "react";
import { createPortal } from "react-dom";
import { useTranslations } from "next-intl";
import { CopyButton } from "./copy-button";

interface CodeBlock {
  host: HTMLElement;
  text: string;
}

/**
 * A copy button for every code block (`<pre>`) of a rendered rich text. The HTML is set by
 * `dangerouslySetInnerHTML`, so React doesn't own its nodes: after rendering, each `<pre>` is wrapped in a
 * `.code-block` (which doesn't scroll, so the button stays in the corner of a long line) with a host for the
 * button, rendered there by a portal. New HTML replaces all of it, the wrapping then starts over. Inline code gets
 * no button. Only the DOM is changed, after sanitizing — nothing of it is parsed from the stored text.
 */
export function useCodeBlockCopyButtons(
  ref: RefObject<HTMLElement | null>,
  html: string
): ReactNode {
  const t = useTranslations();
  const [blocks, setBlocks] = useState<CodeBlock[]>([]);

  useLayoutEffect(() => {
    const container = ref.current;
    if (!container) return;
    const found = Array.from(container.querySelectorAll("pre")).map((pre) => {
      const wrapper = pre.parentElement;
      // Already wrapped, when the effect runs twice for the same HTML (strict mode).
      let host = wrapper?.classList.contains("code-block")
        ? wrapper.querySelector<HTMLElement>(":scope > .code-block-copy")
        : null;
      if (!host) {
        const block = document.createElement("div");
        block.className = "code-block";
        pre.before(block);
        block.append(pre);
        host = document.createElement("span");
        host.className = "code-block-copy";
        block.append(host);
      }
      return { host, text: pre.textContent ?? "" };
    });
    setBlocks(found);
  }, [ref, html]);

  return blocks.map(({ host, text }, index) =>
    createPortal(
      <CopyButton
        value={text}
        label={t("richTextEditor.codeBlock")}
        variant="ghost"
        className="size-7"
        iconSize={14}
      />,
      host,
      index
    )
  );
}
