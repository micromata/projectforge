import React, { PropsWithChildren, useLayoutEffect, useState } from 'react';
import { Router } from 'react-router';
import history from '../utilities/history';

type CustomRouterProps = PropsWithChildren<Record<string, never>>

function CustomRouter({ children }: CustomRouterProps) {
    const [state, setState] = useState({
        action: history.action,
        location: history.location,
    });

    useLayoutEffect(() => {
        const unlisten = history.listen(setState);

        return () => {
            unlisten();
        };
    }, [history]);

    return (
        <Router location={state.location} navigator={history} navigationType={state.action}>
            {children}
        </Router>
    );
}

export default CustomRouter;
