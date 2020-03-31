import { TOAST_ADD, TOAST_CLEAR_ALL, TOAST_REMOVE, USER_LOGIN_BEGIN } from '../actions';

const initialState = [];

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case TOAST_ADD:
            return [
                ...state,
                { ...payload },
            ];
        case TOAST_REMOVE:
            return [
                ...state.map((toast) => {
                    if (toast.id === payload.id) {
                        return {
                            ...toast,
                            dismissed: true,
                        };
                    }

                    return toast;
                }),
            ];
        case USER_LOGIN_BEGIN:
        case TOAST_CLEAR_ALL:
            return initialState;
        default:
            return state;
    }
};

export default reducer;
