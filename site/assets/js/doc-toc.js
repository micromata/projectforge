/*
 * Docs (_layouts/doc.html) and the changelog (_layouts/changelog.html, marked data-changelog-toc, without navigation
 * and chapters: its table of contents lists the majors and their releases, built from the page):
 * - The heading of the current chapter sticks below the header (CSS); it gets the class pf-stuck while it does, for
 *   its shadow.
 * - The table of contents of Asciidoctor (#toc) is copied into the column beside the text (wide screens, where it
 *   replaces the block in the text) or the drawer opened by a floating button (narrow screens), with the chapters
 *   and their sections. The section in view is highlighted, only the sections of its chapter are expanded.
 * - The navigation (left) and the column of the table of contents can be hidden and shown again; the choice is kept
 *   in the local storage (keys pf-doc-nav and pf-doc-toc, also read by the inline script of the layout to avoid a
 *   jump on load).
 */
(function () {
    'use strict';

    var HEADER_HEIGHT = 72; // The sticky header, see .prose .sect1 > h2 in _css/site.css.
    var ACTIVE_OFFSET = 160; // A heading counts as passed above this line (below the header and the sticky heading).

    var layout = document.querySelector('.pf-doc');
    if (!layout) {
        return;
    }
    var wide = window.matchMedia('(min-width: 80rem)'); // xl: the table of contents is a column, not a drawer.

    function store(key, hidden) {
        try {
            if (hidden) {
                localStorage.setItem(key, 'hidden');
            } else {
                localStorage.removeItem(key);
            }
        } catch (e) {
            // Without storage the choice only lasts for this page.
        }
    }

    // Hides or shows a column; the focus moves to the button that undoes it.
    function columnToggle(hideButton, showButton, className, key) {
        hideButton.addEventListener('click', function () {
            layout.classList.add(className);
            store(key, true);
            showButton.focus();
        });
        showButton.addEventListener('click', function () {
            layout.classList.remove(className);
            store(key, false);
            hideButton.focus();
        });
    }

    if (layout.querySelector('[data-nav-hide]')) {
        columnToggle(layout.querySelector('[data-nav-hide]'), layout.querySelector('[data-nav-show]'),
            'pf-doc-nav-hidden', 'pf-doc-nav');
    }

    // Changelog: a list of the majors (articles) with their releases (headings), in the markup of Asciidoctor's TOC.
    function changelogList() {
        var root = document.createElement('ul');
        root.className = 'sectlevel1';
        function item(list, target, text) {
            var li = document.createElement('li');
            var a = document.createElement('a');
            a.href = '#' + target.id;
            a.textContent = text;
            li.appendChild(a);
            list.appendChild(li);
            return li;
        }
        document.querySelectorAll('.pf-changelog-major').forEach(function (major) {
            var title = major.querySelector('h2');
            var li = item(root, major, title ? title.textContent.trim() : major.id);
            var releases = major.querySelectorAll('.changelog-release[id]');
            if (releases.length) {
                var sub = document.createElement('ul');
                sub.className = 'sectlevel2';
                releases.forEach(function (release) {
                    // "ProjectForge 9.2.5: headline" → "ProjectForge 9.2.5"
                    item(sub, release, release.textContent.trim().split(':')[0]);
                });
                li.appendChild(sub);
            }
        });
        return root;
    }

    var chapters = Array.prototype.slice.call(document.querySelectorAll('.prose .sect1 > h2, .pf-changelog-major > h2'));
    var toc = layout.querySelector('.prose #toc') || (layout.hasAttribute('data-changelog-toc') ? layout : null);
    var nav = layout.querySelector('.pf-doc-toc-nav');

    // The links of the column (or drawer) with their headings in document order.
    var entries = [];
    if (toc && nav) {
        var list = toc === layout ? changelogList() : toc.querySelector('ul.sectlevel1');
        if (list) {
            list = list.cloneNode(true);
            // Chapters and their sections only, the deeper levels stay in the text.
            list.querySelectorAll('ul.sectlevel3, ul.sectlevel4, ul.sectlevel5').forEach(function (ul) {
                ul.parentNode.removeChild(ul);
            });
            nav.appendChild(list);
            list.querySelectorAll('a[href^="#"]').forEach(function (link) {
                var target = document.getElementById(decodeURIComponent(link.getAttribute('href').substring(1)));
                if (target) {
                    entries.push({link: link, target: target, chapter: link.closest('.sectlevel1 > li')});
                }
            });
        }
        var hideButton = layout.querySelector('[data-toc-hide]');
        var showButton = layout.querySelector('[data-toc-show]');
        var setDrawer = function (open) {
            layout.classList.toggle('pf-doc-toc-open', open);
            showButton.setAttribute('aria-expanded', open ? 'true' : 'false');
        };
        // Wide, the buttons hide and show the column, narrow they close and open the drawer.
        hideButton.addEventListener('click', function () {
            if (wide.matches) {
                layout.classList.add('pf-doc-toc-hidden');
                store('pf-doc-toc', true);
            } else {
                setDrawer(false);
            }
            showButton.focus();
        });
        showButton.addEventListener('click', function (event) {
            event.stopPropagation(); // Not a click outside the drawer, see below.
            if (wide.matches) {
                layout.classList.remove('pf-doc-toc-hidden');
                store('pf-doc-toc', false);
            } else {
                setDrawer(true);
                keepVisible();
            }
            hideButton.focus();
        });
        // The drawer closes on a click outside, on a link in it, with Escape and when the window gets wide.
        document.addEventListener('click', function (event) {
            if (layout.classList.contains('pf-doc-toc-open') &&
                (!nav.contains(event.target) || event.target.closest('a'))) {
                setDrawer(false);
            }
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && layout.classList.contains('pf-doc-toc-open')) {
                setDrawer(false);
                showButton.focus();
            }
        });
        wide.addEventListener('change', function () {
            setDrawer(false);
        });
    }

    if (!chapters.length && !entries.length) {
        return;
    }

    // The browser scrolls to the position a sticky heading has now: for a chapter above, that is the end of the
    // chapter, where its heading was pushed out. So links to a chapter scroll to its section, which doesn't stick.
    document.addEventListener('click', function (event) {
        var link = event.target.closest && event.target.closest('a[href^="#"]');
        if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey ||
            event.shiftKey || event.altKey) {
            return;
        }
        var id = decodeURIComponent(link.getAttribute('href').substring(1));
        var heading = id && document.getElementById(id);
        if (!heading || chapters.indexOf(heading) < 0) {
            return;
        }
        event.preventDefault();
        heading.parentNode.scrollIntoView();
        if (location.hash !== '#' + id) {
            history.pushState(null, '', '#' + id);
        }
        // Like the browser does, the next Tab starts at the heading.
        heading.setAttribute('tabindex', '-1');
        heading.focus({preventScroll: true});
    });

    var current = null;

    function setCurrent(entry) {
        if (entry === current) {
            return;
        }
        if (current) {
            current.link.removeAttribute('aria-current');
            if (current.chapter) {
                current.chapter.classList.remove('pf-toc-open');
            }
        }
        current = entry;
        if (!entry) {
            return;
        }
        entry.link.setAttribute('aria-current', 'location');
        if (entry.chapter) {
            entry.chapter.classList.add('pf-toc-open');
        }
        keepVisible();
    }

    // Keeps the highlighted link visible in the column (without scrolling the page as scrollIntoView would).
    function keepVisible() {
        if (!current || nav.scrollHeight <= nav.clientHeight) {
            return;
        }
        var link = current.link.getBoundingClientRect();
        var box = nav.getBoundingClientRect();
        if (link.top < box.top + 40) {
            nav.scrollTop += link.top - box.top - 40;
        } else if (link.bottom > box.bottom - 40) {
            nav.scrollTop += link.bottom - box.bottom + 40;
        }
    }

    function update() {
        chapters.forEach(function (heading) {
            var section = heading.parentNode.getBoundingClientRect();
            heading.classList.toggle('pf-stuck', section.top < HEADER_HEIGHT && section.bottom > HEADER_HEIGHT);
        });
        if (entries.length) {
            var active = null;
            for (var i = 0; i < entries.length; i++) {
                if (entries[i].target.getBoundingClientRect().top > ACTIVE_OFFSET) {
                    break;
                }
                active = entries[i];
            }
            // At the end of the page the last sections can't reach the line: take the last one.
            if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2) {
                active = entries[entries.length - 1];
            }
            setCurrent(active);
        }
    }

    var scheduled = false;
    function schedule() {
        if (!scheduled) {
            scheduled = true;
            window.requestAnimationFrame(function () {
                scheduled = false;
                update();
            });
        }
    }

    window.addEventListener('scroll', schedule, {passive: true});
    window.addEventListener('resize', schedule);
    update();
})();
