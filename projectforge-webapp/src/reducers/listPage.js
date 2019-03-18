import { LIST_PAGE_LOAD_BEGIN, LIST_PAGE_LOAD_FAILURE, LIST_PAGE_LOAD_SUCCESS } from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    layout: {},
    filter: {},
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
                layout: payload.layout,
                filter: payload.filter,
                data: payload.data,
            };
        case LIST_PAGE_LOAD_FAILURE:
            return {
                ...initialState,
                loading: false,
                error: payload.error,
            };
        default:
            return state;
    }
};

export default reducer;
