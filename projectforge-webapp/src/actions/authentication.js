export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';
export const USER_LOGOUT = 'USER_LOGOUT';

export const userLoginBegin = keepSignedIn => ({
    type: USER_LOGIN_BEGIN,
    payload: {
        keepSignedIn
    }
});

export const userLoginSuccess = (userId, authenticationToken) => ({
    type: USER_LOGIN_SUCCESS,
    payload: {
        userId,
        authenticationToken
    }
});

export const userLoginFailure = error => ({
    type: USER_LOGIN_FAILURE,
    payload: {
        error
    }
});

export const userLogout = () => ({
    type: USER_LOGOUT
});
