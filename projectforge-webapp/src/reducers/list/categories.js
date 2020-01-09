import {
    LIST_CALL_SUCCESS,
    LIST_DISMISS_ERROR,
    LIST_FAVORITES_RECEIVED,
    LIST_FETCH_DATA_BEGIN,
    LIST_FETCH_FAILURE,
    LIST_FILTER_ADD,
    LIST_FILTER_REMOVE,
    LIST_FILTER_RESET,
    LIST_FILTER_SEARCH_STRING_CHANGED,
    LIST_FILTER_SET,
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
        case LIST_DISMISS_ERROR:
            return {
                ...state,
                error: undefined,
            };
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
                isFetching: true,
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
        case LIST_FILTER_ADD: {
            const { filter } = state;

            return {
                ...state,
                filter: {
                    ...filter,
                    entries: [
                        ...filter.entries,
                        {
                            field: payload.fieldId,
                            isNew: true,
                        },
                    ],
                },
            };
        }
        case LIST_FILTER_REMOVE: {
            const { filter } = state;

            return {
                ...state,
                filter: {
                    ...filter,
                    entries: filter.entries
                        .filter(({ field }) => field !== payload.fieldId),
                },
            };
        }
        case LIST_FILTER_RESET:
            return {
                ...state,
                filter: {
                    ...state.filter,
                    searchString: '',
                    entries: [],
                },
            };
        case LIST_FILTER_SEARCH_STRING_CHANGED:
            return {
                ...state,
                filter: {
                    ...state.filter,
                    searchString: payload.searchString,
                },
            };
        case LIST_FILTER_SET: {
            const { filter } = state;


            return {
                ...state,
                filter: {
                    ...filter,
                    entries: [
                        ...filter.entries
                            .filter(({ field }) => field !== payload.fieldId),
                        {
                            field: payload.fieldId,
                            value: payload.newValue,
                        },
                    ],
                },
            };
        }
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
        case LIST_FAVORITES_RECEIVED:
            return {
                ...state,
                ...payload.response,
            };
        default:
            return state;
    }
};

const reducer = (state = initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LIST_DISMISS_ERROR:
        case LIST_INITIAL_CALL_BEGIN:
        case LIST_FETCH_DATA_BEGIN:
        case LIST_FETCH_FAILURE:
        case LIST_CALL_SUCCESS:
        case LIST_FILTER_ADD:
        case LIST_FILTER_REMOVE:
        case LIST_FILTER_RESET:
        case LIST_FILTER_SEARCH_STRING_CHANGED:
        case LIST_FILTER_SET:
        case LIST_FILTER_SORT:
        case LIST_FAVORITES_RECEIVED: {
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
