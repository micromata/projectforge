import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

interface FilterPortalProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    children: React.ReactNode;
}

export default function FilterPortal({ anchorEl, onClose, children }: FilterPortalProps) {
    const [style, setStyle] = useState<React.CSSProperties>({});
    const portalRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!anchorEl) return;
        const rect = anchorEl.getBoundingClientRect();
        const spaceBelow = window.innerHeight - rect.bottom;
        const flipUp = spaceBelow < 300;
        setStyle({
            position: 'fixed',
            top: flipUp ? undefined : rect.bottom + 4,
            bottom: flipUp ? window.innerHeight - rect.top + 4 : undefined,
            left: rect.left,
            zIndex: 9999,
        });
    }, [anchorEl]);

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
