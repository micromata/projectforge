import {
    EDIT_PAGE_ALL_FIELDS_SET,
    EDIT_PAGE_FIELD_CHANGE,
    EDIT_PAGE_LOAD_BEGIN,
    EDIT_PAGE_LOAD_FAILURE,
    EDIT_PAGE_LOAD_SUCCESS
} from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    ui: {},
    data: {},
    category: '',
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case EDIT_PAGE_LOAD_BEGIN:
            return {
                ...state,
                loading: true,
                error: undefined,
                category: payload.category,
            };
        case EDIT_PAGE_LOAD_SUCCESS:
            return {
                ...state,
                loading: false,
                data: payload.data,
                ui: payload.ui,
            };
        case EDIT_PAGE_LOAD_FAILURE:
            return {
                ...state,
                loading: false,
                error: payload.error,
            };
        case EDIT_PAGE_FIELD_CHANGE:
            return {
                ...state,
                data: {
                    ...state.data,
                    [payload.id]: payload.newValue,
                },
            };
        case EDIT_PAGE_ALL_FIELDS_SET:
            return {
                ...state,
                data: payload.data,
            };
        default:
            return state;
    }
};

export default reducer;
