import { loadList } from './index';

export const LIST_FILTER_RESET = 'LIST_FILTER_RESET';
export const LIST_FILTER_SEARCH_STRING_CHANGED = 'LIST_FILTER_SEARCH_STRING_CHANGED';
export const LIST_FILTER_SORT = 'LIST_FILTER_SORT';

const searchFilterChanged = (category, searchString) => ({
    type: LIST_FILTER_SEARCH_STRING_CHANGED,
    payload: {
        category,
        searchString,
    },
});

const sort = (column, sortProperty, category) => ({
    type: LIST_FILTER_SORT,
    payload: {
        category,
        column,
        sortProperty,
    },
});

const reset = category => ({
    type: LIST_FILTER_RESET,
    payload: { category },
});

export const changeSearchString = searchString => (dispatch, getState) => dispatch(
    searchFilterChanged(getState().list.currentCategory, searchString),
);

export const resetAllFilters = () => (dispatch, getState) => dispatch(
    reset(getState().list.currentCategory),
);

export const sortList = (property, sortOrder) => (dispatch, getState) => {
    let sortProperty = {
        property,
        sortOrder,
    };

    if (!sortOrder) {
        sortProperty.sortOrder = 'ASCENDING';
    } else if (sortOrder === 'DESCENDING') {
        sortProperty = undefined;
    } else {
        sortProperty.sortOrder = 'DESCENDING';
    }

    const category = getState().list.currentCategory;

    dispatch(sort(property, sortProperty, category));
    loadList(category)(dispatch, getState);
};
