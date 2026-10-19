/*
 * Language switch of the header (_includes/header.html, links with data-lang-switch). The link of a translated page
 * leads to the same page in the other language; this keeps the section (#anchor, the same in both languages) and the
 * query (?q= of the search). A page that exists in one language only (data-untranslated) links the home page of the
 * other language: this leads back to the last translated page read in that language instead, e.g. a German reader
 * who followed an (EN) link of the German navigation. Without JavaScript the plain links still work.
 */
(function () {
    'use strict';

    var keyPrefix = 'pf-last-page-';

    function storage() {
        try {
            return window.sessionStorage;
        } catch (e) {
            return null; // Blocked by the browser settings.
        }
    }

    function untranslated() {
        return document.querySelector('[data-lang-switch][data-untranslated]') !== null;
    }

    // Remembers the page on leaving, with the section scrolled to. Untranslated pages (and the 404 page) are no
    // origin to go back to.
    window.addEventListener('pagehide', function () {
        var store = storage();
        if (!store || untranslated()) {
            return;
        }
        store.setItem(keyPrefix + document.documentElement.lang, location.pathname + location.search + location.hash);
    });

    document.addEventListener('click', function (event) {
        var link = event.target.closest && event.target.closest('a[data-lang-switch]');
        if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return;
        }
        var target;
        if (link.hasAttribute('data-untranslated')) {
            var store = storage();
            var last = store && store.getItem(keyPrefix + link.getAttribute('hreflang'));
            // Only a path of this site, never another host (//host).
            if (!last || last.charAt(0) !== '/' || last.charAt(1) === '/') {
                return;
            }
            target = last;
        } else {
            target = link.pathname + location.search + location.hash;
        }
        event.preventDefault();
        location.href = target;
    });
})();
