import {
    LIST_CALL_SUCCESS,
    LIST_FAVORITES_RECEIVED,
    LIST_FETCH_DATA_BEGIN,
    LIST_FETCH_FAILURE,
    LIST_FILTER_SORT,
    LIST_INITIAL_CALL_BEGIN,
} from '../../actions';
import { LIST_FILTER_SEARCH_STRING_CHANGED } from '../../actions/list/filter';

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
        case LIST_FILTER_SEARCH_STRING_CHANGED:
            return {
                ...state,
                filter: {
                    ...state.filter,
                    searchString: payload.searchString,
                },
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
        case LIST_INITIAL_CALL_BEGIN:
        case LIST_FETCH_DATA_BEGIN:
        case LIST_CALL_SUCCESS:
        case LIST_FILTER_SEARCH_STRING_CHANGED:
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
