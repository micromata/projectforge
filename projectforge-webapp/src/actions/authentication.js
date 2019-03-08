import { getServiceURL } from '../utilities/rest';

export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';
export const USER_LOGOUT = 'USER_LOGOUT';

export const userLoginBegin = keepSignedIn => ({
    type: USER_LOGIN_BEGIN,
    payload: {
        keepSignedIn,
    },
});

export const userLoginSuccess = (userId, authenticationToken) => ({
    type: USER_LOGIN_SUCCESS,
    payload: {
        userId,
        authenticationToken,
    },
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

const authenticationUsername = 'Authentication-Username';
const authenticationPassword = 'Authentication-Password';

export const login = (username, password, keepSignedIn) => (dispatch) => {
    dispatch(userLoginBegin(keepSignedIn));
    return fetch(
        getServiceURL('authenticate/getToken'),
        // 'https://httpbin.org/get',
        {
            method: 'GET',
            headers: {
                [authenticationUsername]: username,
                [authenticationPassword]: password,
            },
        },
    )
    // TODO: HANDLE HTTPS ERRORS?
        .then(response => response.json())
        .then((json) => {
            // TODO: SAVE ID AND TOKEN AS COOKIES WHEN KEEPSIGNEDIN IS SET
            dispatch(userLoginSuccess(json.id, json.token));
        })
        .catch(error => dispatch(userLoginFailure(error)));
};
