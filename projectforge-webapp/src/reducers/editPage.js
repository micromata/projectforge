import {
    EDIT_PAGE_FIELD_CHANGE,
    EDIT_PAGE_LOAD_BEGIN,
    EDIT_PAGE_LOAD_FAILURE,
    EDIT_PAGE_LOAD_SUCCESS
} from '../actions';
import { EDIT_PAGE_VALIDATION_HINTS_ENABLE } from '../actions/editPage';

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
        case EDIT_PAGE_VALIDATION_HINTS_ENABLE:
            return {
                ...state,
                validation: {
                    hintsEnabled: true,
                },
            };
        default:
            return state;
    }
};

export default reducer;
