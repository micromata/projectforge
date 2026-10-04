import React, { useEffect } from 'react';
import { useLocation } from 'react-router';
import { getServiceURL } from '../utilities/rest';

/**
 * Full page load of the current url: an old `/wa/...` url (e.g. a stored favorite) reaches the
 * server, whose OrphanedLinkFilter redirects it into projectforge-next. Client-side routing would
 * not find it.
 */
function RedirectToServer() {
    const location = useLocation();

    useEffect(() => {
        if (!import.meta.env.DEV) {
            window.location.reload();
        }
    }, []);

    return (
        <a href={getServiceURL(`..${location.pathname}`)}>Redirect</a>
    );
}

export default RedirectToServer;
