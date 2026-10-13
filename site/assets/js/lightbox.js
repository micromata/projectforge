/*
 * Screenshots open in an overlay on the page instead of a new page, with paging through all screenshots of
 * the page (buttons, arrow keys, swipe). Without JavaScript the links to the full-size images keep working.
 */
(function () {
    'use strict';

    var IMAGE_HREF = /\.(png|jpe?g|gif|webp|svg)(\?.*)?$/i;
    var main = document.getElementById('main');
    if (!main || typeof HTMLDialogElement !== 'function') {
        return;
    }

    // Gallery in document order: images linked to an image file, plus unlinked AsciiDoc and hero screenshots.
    var items = [];
    main.querySelectorAll('img').forEach(function (img) {
        if (!img.alt || img.closest('.pf-flame, header, footer')) {
            return;
        }
        var link = img.closest('a[href]');
        var trigger, src;
        if (link) {
            if (!IMAGE_HREF.test(link.getAttribute('href'))) {
                return;
            }
            trigger = link;
            src = link.href;
        } else if (img.closest('.imageblock, .pf-hero-image')) {
            trigger = img;
            src = img.currentSrc || img.src;
            img.classList.add('pf-zoomable');
            img.setAttribute('role', 'button');
            img.setAttribute('tabindex', '0');
        } else {
            return;
        }
        items.push({trigger: trigger, src: src, alt: img.alt, caption: captionOf(img)});
    });
    if (!items.length) {
        return;
    }

    function captionOf(img) {
        var block = img.closest('.imageblock, figure');
        var title = block && block.querySelector('.title, figcaption');
        return (title && title.textContent.trim()) || img.alt;
    }

    var dialog, image, caption, counter, prevButton, nextButton;
    var index = 0;
    var opener = null;

    function button(className, label, symbol) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'pf-lightbox-button ' + className;
        b.setAttribute('aria-label', label);
        b.innerHTML = symbol;
        return b;
    }

    function build() {
        dialog = document.createElement('dialog');
        dialog.className = 'pf-lightbox';
        dialog.setAttribute('aria-label', 'Screenshot');
        var figure = document.createElement('figure');
        image = document.createElement('img');
        var footer = document.createElement('figcaption');
        caption = document.createElement('span');
        counter = document.createElement('span');
        counter.className = 'pf-lightbox-counter';
        footer.append(caption, counter);
        figure.append(image, footer);
        var closeButton = button('pf-lightbox-close', 'Close', '&#x2715;');
        prevButton = button('pf-lightbox-prev', 'Previous screenshot', '&#x2039;');
        nextButton = button('pf-lightbox-next', 'Next screenshot', '&#x203A;');
        dialog.append(figure, prevButton, nextButton, closeButton);
        document.body.appendChild(dialog);

        closeButton.addEventListener('click', function () { dialog.close(); });
        prevButton.addEventListener('click', function () { show(index - 1); });
        nextButton.addEventListener('click', function () { show(index + 1); });
        // A click beside the image (on the dialog itself, i.e. the dark area) closes it.
        dialog.addEventListener('click', function (event) {
            if (event.target === dialog || event.target === figure) {
                dialog.close();
            }
        });
        dialog.addEventListener('keydown', function (event) {
            if (event.key === 'ArrowLeft') {
                show(index - 1);
                event.preventDefault();
            } else if (event.key === 'ArrowRight') {
                show(index + 1);
                event.preventDefault();
            }
        });
        dialog.addEventListener('close', function () {
            document.documentElement.classList.remove('pf-lightbox-open');
            if (opener) {
                opener.focus();
            }
        });

        var touchX = null;
        dialog.addEventListener('touchstart', function (event) {
            touchX = event.touches.length === 1 ? event.touches[0].clientX : null;
        }, {passive: true});
        dialog.addEventListener('touchend', function (event) {
            if (touchX === null) {
                return;
            }
            var dx = event.changedTouches[0].clientX - touchX;
            touchX = null;
            if (Math.abs(dx) > 50) {
                show(index + (dx < 0 ? 1 : -1));
            }
        });
    }

    function show(i) {
        index = (i + items.length) % items.length;
        var item = items[index];
        image.src = item.src;
        image.alt = item.alt;
        caption.textContent = item.caption;
        counter.textContent = items.length > 1 ? (index + 1) + ' / ' + items.length : '';
        // Preload the neighbours for fast paging.
        if (items.length > 1) {
            new Image().src = items[(index + 1) % items.length].src;
            new Image().src = items[(index - 1 + items.length) % items.length].src;
        }
    }

    function open(i) {
        if (!dialog) {
            build();
        }
        opener = items[i].trigger;
        prevButton.hidden = nextButton.hidden = items.length < 2;
        show(i);
        if (!dialog.open) {
            document.documentElement.classList.add('pf-lightbox-open');
            dialog.showModal();
        }
    }

    items.forEach(function (item, i) {
        item.trigger.addEventListener('click', function (event) {
            // Cmd/Ctrl/Shift click and middle click still open the image in a new tab or window.
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                return;
            }
            event.preventDefault();
            open(i);
        });
        if (item.trigger.tagName === 'IMG') {
            item.trigger.addEventListener('keydown', function (event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    open(i);
                }
            });
        }
    });
})();
