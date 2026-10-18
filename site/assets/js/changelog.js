/*
 * Changelog page: one button opens or closes all collapsed blocks (Overview, All changes, snapshots), so the
 * whole changelog can be searched with the browser's find. Printing opens all blocks, too. Without JavaScript
 * the button stays hidden.
 */
(function () {
    'use strict';

    var button = document.getElementById('changelog-toggle-all');
    var main = document.getElementById('main');
    if (!button || !main) {
        return;
    }
    var blocks = main.querySelectorAll('details');
    if (!blocks.length) {
        return;
    }

    function anyClosed() {
        return Array.prototype.some.call(blocks, function (block) {
            return !block.open;
        });
    }

    function setAll(open) {
        blocks.forEach(function (block) {
            block.open = open;
        });
        updateLabel();
    }

    function updateLabel() {
        var expand = anyClosed();
        button.textContent = expand ? 'Expand all' : 'Collapse all';
        button.setAttribute('aria-expanded', expand ? 'false' : 'true');
    }

    button.addEventListener('click', function () {
        setAll(anyClosed());
    });
    // The toggle event doesn't bubble: capture it to follow blocks opened or closed one by one.
    main.addEventListener('toggle', updateLabel, true);
    window.addEventListener('beforeprint', function () {
        setAll(true);
    });

    updateLabel();
    button.hidden = false;
})();
