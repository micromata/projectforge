import { getServiceURL, handleHTTPErrors, } from '../utilities/rest';

export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';

export const USER_LOGOUT = 'USER_LOGOUT';

export const userLoginBegin = () => ({
    type: USER_LOGIN_BEGIN,
});

export const userLoginSuccess = (user, version) => ({
    type: USER_LOGIN_SUCCESS,
    payload: {
        user,
        version,
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

const catchError = dispatch => error => dispatch(userLoginFailure(error.message));

export const loadUserStatus = () => (dispatch) => {
    dispatch(userLoginBegin());

    return fetch(
        getServiceURL('../rs/userStatus'),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(({ userData, systemData }) => {
            dispatch(userLoginSuccess(userData, systemData.version));
        })
        .catch(catchError(dispatch));
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
        .then(() => loadUserStatus()(dispatch))
        .catch(catchError(dispatch));
};

export const logout = () => dispatch => fetch(getServiceURL('../rs/logout'))
    .then(() => dispatch(userLogout()));
