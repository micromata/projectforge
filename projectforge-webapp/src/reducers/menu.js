import { MENU_LOAD_BEGIN, MENU_LOAD_FAILURE, MENU_LOAD_SUCCESS } from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    categories: [],
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case MENU_LOAD_BEGIN:
            return {
                ...state,
                loading: true,
                error: undefined,
                categories: [],
            };
        case MENU_LOAD_SUCCESS:
            return {
                ...state,
                loading: false,
                categories: payload.menu,
            };
        case MENU_LOAD_FAILURE:
            return {
                ...state,
                loading: false,
                error: payload.error,
            };
        default:
            return state;
    }
};

export default reducer;
