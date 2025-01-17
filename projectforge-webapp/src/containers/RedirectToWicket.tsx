import React, { useEffect } from 'react';
import { useLocation } from 'react-router';
import { getServiceURL } from '../utilities/rest';

function RedirectToWicket() {
    const location = useLocation();

    useEffect(() => {
        if (process.env.NODE_ENV !== 'development') {
            window.location.reload();
        }
    }, []);

    return (
        <a href={getServiceURL(`..${location.pathname}`)}>Redirect to Wicket</a>
    );
}

export default RedirectToWicket;
