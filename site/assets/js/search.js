/*
 * Search page (search.adoc, de/search.adoc): Tipue Search as a live search. It searches while typing (from
 * tipuesearch_set.js' minimum length on, after a short pause) and keeps the term in the url (?q=), so a search
 * can be shared and reloaded. Enter searches at once instead of reloading the page. Tipue shows a loading
 * spinner until its first search; without a term in the url it is removed, the page is empty until typing.
 */
$(function () {
    'use strict';

    var input = $('#tipue_search_input');
    var content = $('#tipue_search_content');
    var minimumLength = 3;
    var timer = null;

    // showTime measures from the page load, not from the search: meaningless while typing.
    input.tipuesearch({ minimumLength: minimumLength, showTime: false });
    if (!input.val()) {
        content.empty();
    }

    // Tipue searches on the keyup of Enter only.
    function search() {
        var term = $.trim(input.val());
        var url = new URL(window.location.href);
        if (term.length < minimumLength) {
            content.empty();
            url.searchParams.delete('q');
        } else {
            input.trigger($.Event('keyup', { keyCode: 13 }));
            url.searchParams.set('q', term);
        }
        window.history.replaceState(null, '', url);
    }

    input.on('input', function () {
        window.clearTimeout(timer);
        timer = window.setTimeout(search, 250);
    });
    input.closest('form').on('submit', function (event) {
        event.preventDefault();
        window.clearTimeout(timer);
        search();
    });
});
