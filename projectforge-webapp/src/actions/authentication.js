import cookies from 'react-cookies';

import { getServiceURL, handleHTTPErrors, } from '../utilities/rest';

export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';

export const USER_LOGOUT = 'USER_LOGOUT';

export const userLoginBegin = () => ({
    type: USER_LOGIN_BEGIN,
});

export const userLoginSuccess = () => ({
    type: USER_LOGIN_SUCCESS,
});

export const userLoginFailure = error => ({
    type: USER_LOGIN_FAILURE,
    payload: {
        error,
    },
});

export const userLogout = () => ({
    type: USER_LOGOUT,
});

const KEEP_SIGNED_IN_COOKIE = 'KEEP_SIGNED_IN';

const removeCookies = () => {
    cookies.remove(KEEP_SIGNED_IN_COOKIE);
};

const catchError = dispatch => (error) => {
    removeCookies();

    dispatch(userLoginFailure(error.message));
};

export const login = (username, password, keepSignedIn) => (dispatch) => {
    dispatch(userLoginBegin());
    return fetch(
        getServiceURL('../publicRest/login'),
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
        .then(() => {
            if (keepSignedIn) {
                cookies.save(KEEP_SIGNED_IN_COOKIE, true);
            }

            dispatch(userLoginSuccess());
        })
        .catch(catchError(dispatch));
};

export const logout = () => (dispatch) => {
    removeCookies();

    dispatch(userLogout());
};

export const loadSessionIfAvailable = () => (dispatch) => {
    const keepSignedIn = cookies.load(KEEP_SIGNED_IN_COOKIE);

    if (!keepSignedIn) {
        return null;
    }

    dispatch(userLoginBegin());

    return fetch(
        // TODO: ADD AUTHENTICATION TEST ENDPOINT
        getServiceURL('authenticate/initialContact'),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(() => dispatch(userLoginSuccess()))
        .catch(catchError(dispatch));
};
