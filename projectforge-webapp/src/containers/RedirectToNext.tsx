import React, { useEffect } from 'react';
import { useLocation } from 'react-router';

/**
 * Leaves this app for projectforge-next (served by Spring under /next).
 *
 * Menu entries migrated to projectforge-next carry a `next/...` url. React Router would otherwise
 * try to resolve them as a category of this app, so they need a real page load instead.
 */
function RedirectToNext() {
    const location = useLocation();
    const target = `${location.pathname}${location.search}`;

    useEffect(() => {
        window.location.replace(target);
    }, [target]);

    return (
        <a href={target}>Redirect to ProjectForge Next</a>
    );
}

export default RedirectToNext;
