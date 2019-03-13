import cookies from 'react-cookies';

import {
    getAuthenticationHeaders,
    getLoginHeaders,
    getServiceURL,
    handleHTTPErrors,
} from '../utilities/rest';

export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';

export const USER_LOGOUT = 'USER_LOGOUT';

const userLoginBegin = () => ({
    type: USER_LOGIN_BEGIN,
});

const userLoginSuccess = (userId, authenticationToken) => ({
    type: USER_LOGIN_SUCCESS,
    payload: {
        userId,
        authenticationToken,
    },
});

const userLoginFailure = error => ({
    type: USER_LOGIN_FAILURE,
    payload: {
        error,
    },
});

const userLogout = () => ({
    type: USER_LOGOUT,
});

const USER_ID_COOKIE = 'USER_ID';
const TOKEN_COOKIE = 'TOKEN';

const removeCookies = () => {
    cookies.remove(USER_ID_COOKIE);
    cookies.remove(TOKEN_COOKIE);
};

const catchError = dispatch => (error) => {
    removeCookies();

    dispatch(userLoginFailure(error));
};

export const login = (username, password, keepSignedIn) => (dispatch) => {
    dispatch(userLoginBegin());
    return fetch(
        getServiceURL('authenticate/getToken'),
        {
            method: 'GET',
            headers: getLoginHeaders(username, password),
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(({ id, authenticationToken }) => {
            if (keepSignedIn) {
                cookies.save(USER_ID_COOKIE, id);
                cookies.save(TOKEN_COOKIE, authenticationToken);
            } else {
                removeCookies();
            }

            dispatch(userLoginSuccess(id, authenticationToken));
        })
        .catch(catchError(dispatch));
};

export const logout = () => (dispatch) => {
    removeCookies();

    dispatch(userLogout());
};

export const loadSessionIfAvailable = () => (dispatch) => {
    const userID = cookies.load(USER_ID_COOKIE);
    const token = cookies.load(TOKEN_COOKIE);

    if (!userID || !token) {
        return;
    }

    dispatch(userLoginBegin());

    fetch(
        getServiceURL('authenticate/initialContact'),
        {
            method: 'GET',
            headers: getAuthenticationHeaders(userID, token),
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(() => dispatch(userLoginSuccess(userID, token)))
        .catch(catchError(dispatch));
};
