import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';

export const userLoginBegin = () => ({
    type: USER_LOGIN_BEGIN,
});

export const userLoginSuccess = (user, version, buildTimestamp, alertMessage) => ({
    type: USER_LOGIN_SUCCESS,
    payload: {
        user,
        version,
        buildTimestamp,
        alertMessage,
    },
});

export const userLoginFailure = (error) => ({
    type: USER_LOGIN_FAILURE,
    payload: {
        error,
    },
});

const catchError = (dispatch) => (error) => dispatch(userLoginFailure(error.message));

export const loadUserStatus = () => (dispatch) => {
    dispatch(userLoginBegin());

    return fetch(
        getServiceURL('userStatus'),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then((response) => response.json())
        .then(({ userData, systemData, alertMessage }) => {
            dispatch(userLoginSuccess(
                userData,
                systemData.version,
                systemData.buildTimestamp,
                alertMessage,
            ));
        })
        .catch(() => {
            const { pathname, search } = window.location;
            const href = pathname + search;
            if (!pathname.startsWith('/react/public/login')
                // /react/public/datatransfer/ is a special case where the user is not logged in.
                // Login form is returned by data transfer.
                && !pathname.startsWith('/react/public/datatransfer/')) {
                // set the URL to redirect to after login:
                window.location.href = `/react/public/login?url=${encodeURIComponent(href)}`;
            }

            catchError(dispatch)({ message: undefined });
        });
};

export const login = (username, password, keepSignedIn) => (dispatch) => {
    dispatch(userLoginBegin());
    return fetch(
        getServiceURL('/rsPublic/login'),
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username,
                password,
                stayLoggedIn: keepSignedIn,
            }),
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(() => loadUserStatus()(dispatch))
        .catch(catchError(dispatch));
};
