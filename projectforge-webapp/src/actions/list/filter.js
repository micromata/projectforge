import { loadList } from './index';

export const LIST_FILTER_ADD = 'LIST_FILTER_ADD';
export const LIST_FILTER_REMOVE = 'LIST_FILTER_REMOVE';
export const LIST_FILTER_RESET = 'LIST_FILTER_RESET';
export const LIST_FILTER_SEARCH_STRING_CHANGED = 'LIST_FILTER_SEARCH_STRING_CHANGED';
export const LIST_FILTER_SET = 'LIST_FILTER_SET';
export const LIST_FILTER_SORT = 'LIST_FILTER_SORT';

const add = (category, fieldId) => ({
    type: LIST_FILTER_ADD,
    payload: {
        category,
        fieldId,
    },
});

const remove = (category, fieldId) => ({
    type: LIST_FILTER_REMOVE,
    payload: {
        category,
        fieldId,
    },
});

const reset = (category) => ({
    type: LIST_FILTER_RESET,
    payload: { category },
});

const searchFilterChanged = (category, searchString) => ({
    type: LIST_FILTER_SEARCH_STRING_CHANGED,
    payload: {
        category,
        searchString,
    },
});

const set = (category, fieldId, newValue) => ({
    type: LIST_FILTER_SET,
    payload: {
        category,
        fieldId,
        newValue,
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

export const addFilter = (fieldId) => (dispatch, getState) => dispatch(
    add(getState().list.currentCategory, fieldId),
);

export const removeFilter = (fieldId) => (dispatch, getState) => {
    const category = getState().list.currentCategory;

    dispatch(remove(category, fieldId));
    loadList(category)(dispatch, getState);
};

export const changeSearchString = (searchString) => (dispatch, getState) => dispatch(
    searchFilterChanged(getState().list.currentCategory, searchString),
);

export const resetAllFilters = () => (dispatch, getState) => dispatch(
    reset(getState().list.currentCategory),
);

export const setFilter = (fieldId, newValue) => (dispatch, getState) => {
    const category = getState().list.currentCategory;

    dispatch(set(category, fieldId, newValue));
    loadList(category)(dispatch, getState);
};

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
