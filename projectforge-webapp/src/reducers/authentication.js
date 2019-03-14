import { USER_LOGIN_BEGIN, USER_LOGIN_FAILURE, USER_LOGIN_SUCCESS, USER_LOGOUT, } from '../actions';

const initialState = {
    loading: false,
    error: null,
    user: null,
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case USER_LOGIN_BEGIN:
            return {
                ...state,
                loading: true,
                error: null,
                user: null,
            };
        case USER_LOGIN_SUCCESS:
            return {
                ...state,
                loading: false,
                error: null,
                user: {
                    id: payload.userId,
                    token: payload.authenticationToken,
                },
            };
        case USER_LOGIN_FAILURE:
            return {
                ...state,
                user: null,
                loading: false,
                error: payload.error,
            };
        case USER_LOGOUT:
            return {
                ...state,
                loading: false,
                error: null,
                user: null,
            };
        default:
            return state;
    }
};

export default reducer;
