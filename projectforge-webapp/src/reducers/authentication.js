import { USER_LOGIN_BEGIN, USER_LOGIN_FAILURE, USER_LOGIN_SUCCESS } from '../actions';

const initialState = {
    loading: true,
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
                user: payload.user,
                version: payload.version,
                buildTimestamp: payload.buildTimestamp,
                alertMessage: payload.alertMessage,
            };
        case USER_LOGIN_FAILURE:
            return {
                ...state,
                user: null,
                loading: false,
                error: payload.error,
            };
        default:
            return state;
    }
};

export default reducer;
