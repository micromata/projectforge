/*
 * Copy button of the code blocks: AsciiDoc listing and literal blocks and Markdown code blocks get a button above the
 * code, right-aligned in the line of the block title (e.g. the file name, `.File: `pforgebackup.sh``), or in a
 * line of its own if the block has no title. It copies the whole content (without the line numbers of
 * [source,…,linenums]). The texts are given by the script tag (data-copy, data-copied, see _layouts/default.html),
 * the icons are those of Lucide (https://lucide.dev, ISC license, see _includes/icons/README.md).
 */
(function () {
    'use strict';

    var script = document.currentScript;
    var main = document.getElementById('main');
    if (!main || !script) {
        return;
    }
    var copyLabel = script.getAttribute('data-copy') || 'Copy to clipboard';
    var copiedLabel = script.getAttribute('data-copied') || 'Copied';

    var SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" ' +
        'stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">';
    var COPY_ICON = SVG + '<rect width="14" height="14" x="8" y="8" rx="2" ry="2"/>' +
        '<path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>';
    var CHECK_ICON = SVG + '<path d="M20 6 9 17l-5-5"/></svg>';

    // The text of a block: with line numbers, the code is the second cell of the table of rouge.
    function codeOf(block) {
        var pre = block.querySelector('td.code pre') || block.querySelector('pre');
        return pre ? pre.textContent.replace(/\n$/, '') : '';
    }

    // Fallback for browsers without the Clipboard API (or pages not served via https or localhost).
    function copyByTextarea(text) {
        var area = document.createElement('textarea');
        area.value = text;
        area.setAttribute('readonly', '');
        area.style.position = 'fixed';
        area.style.opacity = '0';
        document.body.appendChild(area);
        area.select();
        var ok = false;
        try {
            ok = document.execCommand('copy');
        } catch (e) {
            ok = false;
        }
        document.body.removeChild(area);
        return ok ? Promise.resolve() : Promise.reject(new Error('copy failed'));
    }

    function copy(text) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(text).catch(function () {
                return copyByTextarea(text);
            });
        }
        return copyByTextarea(text);
    }

    function setState(button, copied) {
        button.innerHTML = copied ? CHECK_ICON : COPY_ICON;
        button.setAttribute('aria-label', copied ? copiedLabel : copyLabel);
        button.title = copied ? copiedLabel : copyLabel;
        button.classList.toggle('pf-code-copied', copied);
    }

    main.querySelectorAll('.listingblock, .literalblock, div.highlighter-rouge').forEach(function (block) {
        if (!block.querySelector('pre')) {
            return;
        }
        // The title of AsciiDoc is the first child of the block, the button goes into its line.
        var header = block.querySelector(':scope > .title');
        if (!header) {
            header = document.createElement('div');
            header.className = 'pf-code-untitled';
            block.insertBefore(header, block.firstChild);
        }
        header.classList.add('pf-code-header');
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'pf-code-copy';
        setState(button, false);
        var timer;
        button.addEventListener('click', function () {
            copy(codeOf(block)).then(function () {
                setState(button, true);
                clearTimeout(timer);
                timer = setTimeout(function () {
                    setState(button, false);
                }, 2000);
            }).catch(function () {
                // Nothing to do: the code may still be selected and copied by hand.
            });
        });
        header.appendChild(button);
    });
})();
