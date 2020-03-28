import {
    FORM_CALL_ACTION_BEGIN,
    FORM_CALL_ACTION_SUCCESS,
    FORM_CALL_FAILURE,
    FORM_CALL_INITIAL_BEGIN,
    FORM_CALL_SUCCESS,
    FORM_CHANGE_DATA,
    FORM_CHANGE_VARIABLES,
    FORM_SWITCH_CATEGORY,
    USER_LOGIN_BEGIN,
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
        case FORM_CALL_ACTION_BEGIN:
            return {
                ...state,
                isFetching: true,
                error: undefined,
                validationErrors: [],
            };
        case FORM_CALL_ACTION_SUCCESS:
            return {
                ...state,
                isFetching: false,
            };
        case FORM_CALL_FAILURE:
            return {
                ...state,
                isFetching: false,
                error: payload.error,
            };
        case FORM_CALL_INITIAL_BEGIN: {
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
        case FORM_CALL_SUCCESS:
            return {
                ...state,
                isFetching: false,
                ...payload.response,
            };
        case FORM_CHANGE_DATA:
            return {
                ...state,
                data: Object.combine(state.data, payload.newData),
            };
        case FORM_CHANGE_VARIABLES:
            return {
                ...state,
                variables: {
                    ...state.variables,
                    ...payload.newVariables,
                },
            };
        case FORM_SWITCH_CATEGORY:
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
        case USER_LOGIN_BEGIN:
            return initialState;
        case FORM_CALL_ACTION_BEGIN:
        case FORM_CALL_ACTION_SUCCESS:
        case FORM_CALL_FAILURE:
        case FORM_CALL_INITIAL_BEGIN:
        case FORM_CALL_SUCCESS:
        case FORM_CHANGE_DATA:
        case FORM_CHANGE_VARIABLES:
        case FORM_SWITCH_CATEGORY: {
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
