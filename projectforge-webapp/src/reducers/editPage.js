import {
    EDIT_PAGE_FIELD_CHANGE,
    EDIT_PAGE_LOAD_BEGIN,
    EDIT_PAGE_LOAD_FAILURE,
    EDIT_PAGE_LOAD_SUCCESS,
    EDIT_PAGE_UPDATE_BEGIN,
    EDIT_PAGE_UPDATE_FAILURE,
} from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    ui: {},
    data: {},
    variables: {},
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
                validation: undefined,
            };
        case EDIT_PAGE_LOAD_SUCCESS:
            return {
                ...state,
                loading: false,
                data: payload.data,
                variables: payload.variables,
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
        case EDIT_PAGE_UPDATE_BEGIN:
            return {
                ...state,
                loading: true,
            };
        case EDIT_PAGE_UPDATE_FAILURE:
            return {
                ...state,
                loading: false,
                validation: payload.validationMessages,
            };
        default:
            return state;
    }
};

export default reducer;
