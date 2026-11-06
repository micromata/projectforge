/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

/**
 * The search in the page, in JavaScript, on Android: react-native-webview has no API for it (no
 * WebView.findAllAsync). iOS has the find navigator of WebKit (PfNativeModule.presentFindNavigator): the highlights of
 * this script were painted wrong there (whole words, the marks of earlier queries kept). The matches are marked with the CSS Custom Highlight API
 * (Safari 17.2+, Chrome 105+), which leaves the DOM alone: wrapping them into elements of its own (the way of most
 * scripts, and of flutter_inappwebview without the navigator) breaks the DOM React of next manages. The page's CSP
 * doesn't apply to scripts the app evaluates; the style element needs 'unsafe-inline' for styles (given).
 *
 * The script answers by postMessage: `{ pf: 'find', count, index }`.
 */
export function findScript(query: string, direction: 'first' | 'next' | 'previous' | 'clear'): string {
  return `(function () {
  var state = window.__pfFind || (window.__pfFind = { ranges: [], index: -1, query: '' });
  var supported = typeof CSS !== 'undefined' && CSS.highlights && typeof Highlight !== 'undefined';
  function answer() {
    window.ReactNativeWebView && window.ReactNativeWebView.postMessage(JSON.stringify({
      pf: 'find', count: state.ranges.length, index: state.index, supported: !!supported }));
  }
  if (!supported) { answer(); return; }
  if (!document.getElementById('pf-find-style')) {
    var style = document.createElement('style');
    style.id = 'pf-find-style';
    style.textContent = '::highlight(pf-find){background-color:rgba(255,213,0,.55)}' +
      '::highlight(pf-find-current){background-color:rgba(255,140,0,.9)}';
    (document.head || document.documentElement).appendChild(style);
  }
  var query = ${JSON.stringify(query)}, direction = ${JSON.stringify(direction)};
  if (direction === 'clear' || !query) {
    CSS.highlights.delete('pf-find'); CSS.highlights.delete('pf-find-current');
    state.ranges = []; state.index = -1; state.query = ''; answer(); return;
  }
  if (direction === 'first' || query !== state.query) {
    state.ranges = []; state.query = query;
    var needle = query.toLocaleLowerCase();
    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, { acceptNode: function (node) {
      var parent = node.parentElement;
      if (!parent || /^(SCRIPT|STYLE|NOSCRIPT)$/.test(parent.tagName)) return NodeFilter.FILTER_REJECT;
      if (!parent.getClientRects().length) return NodeFilter.FILTER_REJECT;
      return NodeFilter.FILTER_ACCEPT;
    } });
    for (var node = walker.nextNode(); node; node = walker.nextNode()) {
      var text = node.data.toLocaleLowerCase(), at = text.indexOf(needle);
      while (at >= 0 && state.ranges.length < 1000) {
        var range = document.createRange(); range.setStart(node, at); range.setEnd(node, at + query.length);
        state.ranges.push(range); at = text.indexOf(needle, at + needle.length);
      }
    }
    state.index = state.ranges.length ? 0 : -1;
  } else if (state.ranges.length) {
    var n = state.ranges.length;
    state.index = (state.index + (direction === 'previous' ? n - 1 : 1)) % n;
  }
  CSS.highlights.set('pf-find', new Highlight(...state.ranges));
  if (state.index >= 0) {
    var current = state.ranges[state.index];
    CSS.highlights.set('pf-find-current', new Highlight(current));
    var element = current.startContainer.parentElement;
    element && element.scrollIntoView({ block: 'center', behavior: 'smooth' });
  } else { CSS.highlights.delete('pf-find-current'); }
  answer();
})(); true;`;
}
