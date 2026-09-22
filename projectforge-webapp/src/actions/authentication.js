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
            // /react/public/datatransfer/ is a special case where the user is not logged in.
            // Login form is returned by data transfer.
            if (!pathname.startsWith('/react/public/datatransfer/')) {
                // The login lives in projectforge-next, so this leaves this app for good:
                // set the URL to return to after the login.
                window.location.href = `/next/login?returnUrl=${encodeURIComponent(href)}`;
            }

            catchError(dispatch)({ message: undefined });
        });
};
