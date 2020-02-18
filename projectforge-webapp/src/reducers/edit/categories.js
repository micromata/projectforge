import {
    EDIT_CALL_ACTION_BEGIN,
    EDIT_CALL_ACTION_SUCCESS,
    EDIT_CALL_FAILURE,
    EDIT_CALL_INITIAL_BEGIN,
    EDIT_CALL_SUCCESS,
    EDIT_CHANGE_DATA,
    EDIT_CHANGE_VARIABLES,
    EDIT_SWITCH_CATEGORY,
} from '../../actions';

const initialState = {};
const initialCategoryState = {
    isFetching: false,
    data: {},
    ui: { translations: {} },
    validationErrors: [],
    variables: {},
    watchFieldsTriggered: [],
};

const categoryReducer = (state = initialCategoryState, { type, payload }) => {
    switch (type) {
        case EDIT_CALL_ACTION_BEGIN:
            return {
                ...state,
                isFetching: true,
                error: undefined,
                validationErrors: [],
            };
        case EDIT_CALL_ACTION_SUCCESS:
            return {
                ...state,
                isFetching: false,
            };
        case EDIT_CALL_FAILURE:
            return {
                ...state,
                isFetching: false,
                error: payload.error,
            };
        case EDIT_CALL_INITIAL_BEGIN: {
            let { data } = state;

            // Clear data if its another entry or a new entry.
            if (state.id !== payload.id || payload.id === undefined) {
                data = {};
            }

            return {
                ...state,
                isFetching: true,
                id: payload.id,
                data,
                validationErrors: [],
                watchFieldsTriggered: [],
                error: undefined,
            };
        }
        case EDIT_CALL_SUCCESS:
            return {
                ...state,
                isFetching: false,
                ...payload.response,
            };
        case EDIT_CHANGE_DATA:
            return {
                ...state,
                data: {
                    ...state.data,
                    ...payload.newData,
                },
            };
        case EDIT_CHANGE_VARIABLES:
            return {
                ...state,
                variables: {
                    ...state.variables,
                    ...payload.newVariables,
                },
            };
        case EDIT_SWITCH_CATEGORY:
            return {
                ...state,
                isFetching: false,
                ...payload.newVariables,
            };
        default:
            return state;
    }
};

const reducer = (state = initialState, action) => {
    const { type, payload } = action;

    switch (type) {
        case EDIT_CALL_ACTION_BEGIN:
        case EDIT_CALL_ACTION_SUCCESS:
        case EDIT_CALL_FAILURE:
        case EDIT_CALL_INITIAL_BEGIN:
        case EDIT_CALL_SUCCESS:
        case EDIT_CHANGE_DATA:
        case EDIT_CHANGE_VARIABLES:
        case EDIT_SWITCH_CATEGORY: {
            const { category } = payload;

            if (category) {
                return {
                    ...state,
                    [category]: categoryReducer(state[category], action),
                };
            }

            break;
        }
        default:
    }

    return state;
};

export default reducer;
