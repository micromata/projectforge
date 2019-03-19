import {
    LIST_PAGE_FILTER_SET,
    LIST_PAGE_LOAD_BEGIN,
    LIST_PAGE_LOAD_FAILURE,
    LIST_PAGE_LOAD_SUCCESS,
} from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    ui: {},
    data: [],
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case LIST_PAGE_LOAD_BEGIN:
            return {
                ...initialState,
                loading: true,
            };
        case LIST_PAGE_LOAD_SUCCESS:
            return {
                ...initialState,
                loading: false,
                filter: payload.filter,
                ui: payload.ui,
                data: payload.data,
            };
        case LIST_PAGE_LOAD_FAILURE:
            return {
                ...initialState,
                loading: false,
                error: payload.error,
            };
        case LIST_PAGE_FILTER_SET:
            return {
                ...state,
                filter: {
                    ...state.filter,
                    [payload.id]: payload.newValue,
                },
            };
        default:
            return state;
    }
};

export default reducer;
