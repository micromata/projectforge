import React, {
    useEffect, useLayoutEffect, useRef, useState,
} from 'react';
import { createPortal } from 'react-dom';

interface FilterPortalProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    children: React.ReactNode;
}

const MARGIN = 4;

export default function FilterPortal({ anchorEl, onClose, children }: FilterPortalProps) {
    // Hidden until measured, so the panel doesn't flash at the wrong position before flipping.
    const [style, setStyle] = useState<React.CSSProperties>({
        position: 'fixed',
        visibility: 'hidden',
        top: 0,
        left: 0,
        zIndex: 9999,
    });
    const portalRef = useRef<HTMLDivElement>(null);

    // Position the panel after it has rendered, so we know its actual width/height and can flip it
    // vertically (when it would overflow the bottom) and horizontally (when it would overflow the
    // right edge, e.g. for the rightmost column).
    useLayoutEffect(() => {
        if (!anchorEl || !portalRef.current) return;
        const rect = anchorEl.getBoundingClientRect();
        const panelWidth = portalRef.current.offsetWidth;

        const spaceBelow = window.innerHeight - rect.bottom;
        const flipUp = spaceBelow < 300;

        // Prefer aligning the panel's left edge to the anchor; flip to right-aligned if it would
        // overflow the viewport's right edge.
        let leftPos: number | undefined = rect.left;
        let rightPos: number | undefined;
        if (rect.left + panelWidth > window.innerWidth - MARGIN) {
            rightPos = Math.max(MARGIN, window.innerWidth - rect.right);
            leftPos = undefined;
            // If right-aligning would push the panel off the left edge, clamp it to the viewport.
            if (window.innerWidth - rightPos - panelWidth < MARGIN) {
                rightPos = undefined;
                leftPos = MARGIN;
            }
        }

        setStyle({
            position: 'fixed',
            top: flipUp ? undefined : rect.bottom + MARGIN,
            bottom: flipUp ? window.innerHeight - rect.top + MARGIN : undefined,
            left: leftPos,
            right: rightPos,
            zIndex: 9999,
        });
    }, [anchorEl, children]);

    useEffect(() => {
        // Delay registration so the opening click doesn't immediately close the portal
        const frameId = requestAnimationFrame(() => {
            document.addEventListener('mousedown', handler);
            document.addEventListener('keydown', escHandler);
        });

        function handler(e: MouseEvent) {
            const target = e.target as Node;
            if (portalRef.current?.contains(target)) return;
            if (anchorEl?.contains(target)) return;
            onClose();
        }
        function escHandler(e: KeyboardEvent) {
            if (e.key === 'Escape') onClose();
        }

        return () => {
            cancelAnimationFrame(frameId);
            document.removeEventListener('mousedown', handler);
            document.removeEventListener('keydown', escHandler);
        };
    }, [anchorEl, onClose]);

    if (!anchorEl) return null;

    return createPortal(
        <div ref={portalRef} style={style}>{children}</div>,
        document.body,
    );
}
