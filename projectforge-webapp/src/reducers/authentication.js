import {
    USER_LOGIN_BEGIN,
    USER_LOGIN_FAILURE,
    USER_LOGIN_SUCCESS,
    USER_LOGOUT,
} from '../actions';

const initialState = {
    loading: false,
    error: null,
    loggedIn: false,
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case USER_LOGIN_BEGIN:
            return {
                ...state,
                loading: true,
                error: null,
                loggedIn: false,
            };
        case USER_LOGIN_SUCCESS:
            return {
                ...state,
                loading: false,
                error: null,
                loggedIn: true,
            };
        case USER_LOGIN_FAILURE:
            return {
                ...state,
                loggedIn: false,
                loading: false,
                error: payload.error,
            };
        case USER_LOGOUT:
            return {
                ...state,
                loading: false,
                error: null,
                loggedIn: false,
            };
        default:
            return state;
    }
};

export default reducer;
