import {
  Extension,
  combineTransactionSteps,
  getChangedRanges,
  getMarkRange,
} from "@tiptap/core";
import { Plugin, PluginKey } from "@tiptap/pm/state";
import { findUrls } from "./linkify-html";

/**
 * Keeps the target of a link in step with its text while the text is a url: the autolink of TipTap links a url
 * once and keeps its href when the url is edited afterwards (`www.a.org` changed to `www.b.org` would still lead
 * to a.org), so the message would show one address and lead to another, and its link preview would stay the old
 * one. Only links edited by the transaction are changed; a link whose text isn't a url (e.g. "here") keeps its
 * target.
 */
export const LinkTextSync = Extension.create({
  name: "linkTextSync",

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: new PluginKey("linkTextSync"),
        appendTransaction: (transactions, oldState, newState) => {
          if (
            !transactions.some((it) => it.docChanged) ||
            transactions.some((it) => it.getMeta("preventAutolink"))
          ) {
            return null;
          }
          const type = newState.schema.marks.link;
          if (!type) return null;
          const { doc } = newState;
          const tr = newState.tr;
          const done = new Set<number>();
          const transform = combineTransactionSteps(oldState.doc, [
            ...transactions,
          ]);
          getChangedRanges(transform).forEach(({ newRange }) => {
            const from = Math.max(0, newRange.from - 1);
            const to = Math.min(doc.content.size, newRange.to + 1);
            doc.nodesBetween(from, to, (node, pos) => {
              if (!node.isText) return;
              const mark = type.isInSet(node.marks);
              if (!mark) return;
              const range = getMarkRange(doc.resolve(pos), type, mark.attrs);
              if (!range || done.has(range.from)) return;
              done.add(range.from);
              const text = doc.textBetween(range.from, range.to);
              const urls = findUrls(text);
              if (
                urls.length !== 1 ||
                urls[0].start !== 0 ||
                urls[0].end !== text.length ||
                urls[0].href === mark.attrs.href
              ) {
                return;
              }
              tr.removeMark(range.from, range.to, type).addMark(
                range.from,
                range.to,
                type.create({ ...mark.attrs, href: urls[0].href })
              );
            });
          });
          return tr.steps.length ? tr : null;
        },
      }),
    ];
  },
});
