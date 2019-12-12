import {
    LIST_CALL_SUCCESS,
    LIST_FETCH_DATA_BEGIN,
    LIST_FETCH_FAILURE,
    LIST_FILTER_SORT,
    LIST_INITIAL_CALL_BEGIN,
} from '../../actions';

const initialState = {};
const initialCategoryState = {
    isFetching: false,
    ui: { translations: {} },
    data: {},
    filter: {
        entries: [],
        extended: {},
    },
    filterFavorites: [],
    variables: {},
};

const categoryReducer = (state = initialCategoryState, { type, payload }) => {
    switch (type) {
        case LIST_INITIAL_CALL_BEGIN:
            return {
                ...state,
                isFetching: true,
                search: payload.search,
                error: undefined,
            };
        case LIST_FETCH_DATA_BEGIN:
            return {
                ...state,
                error: undefined,
            };
        case LIST_CALL_SUCCESS:
            return {
                ...state,
                isFetching: false,
                ...payload.response,
            };
        case LIST_FETCH_FAILURE:
            return {
                ...state,
                isFetching: false,
                error: payload.error,
            };
        case LIST_FILTER_SORT: {
            const { filter } = state;

            return {
                ...state,
                filter: {
                    ...filter,
                    sortProperties: [
                        payload.sortProperty,
                        ...(filter.sortProperties || [])
                            .filter(entry => entry.property !== payload.column)
                            .slice(0, 2),
                    ].filter(entry => entry !== undefined),
                },
            };
        }
        default:
            return state;
    }
};

const reducer = (state = initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LIST_INITIAL_CALL_BEGIN:
        case LIST_FETCH_DATA_BEGIN:
        case LIST_CALL_SUCCESS:
        case LIST_FILTER_SORT: {
            const { category } = payload;

            return {
                ...state,
                [category]: categoryReducer(state[category], action),
            };
        }
        default:
            return state;
    }
};

export default reducer;
