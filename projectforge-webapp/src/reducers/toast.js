import { TOAST_ADD, TOAST_CLEAR_ALL, TOAST_REMOVE } from '../actions';

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
        case TOAST_CLEAR_ALL:
            return [
                ...state.map(toast => ({
                    ...toast,
                    dismissed: true,
                })),
            ];
        default:
            return state;
    }
};

export default reducer;
