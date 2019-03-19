import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const LIST_PAGE_LOAD_BEGIN = 'LIST_PAGE_LOAD_BEGIN';
export const LIST_PAGE_LOAD_SUCCESS = 'LIST_PAGE_LOAD_SUCCESS';
export const LIST_PAGE_LOAD_FAILURE = 'LIST_PAGE_LOAD_FAILURE';

export const LIST_PAGE_FILTER_SET = 'LIST_PAGE_FILTER_SET';

export const LIST_PAGE_FILTER_RESET_BEGIN = 'LIST_PAGE_FILTER_RESET_BEGIN';
export const LIST_PAGE_FILTER_RESET_SUCCESS = 'LIST_PAGE_FILTER_RESET_SUCCESS';

export const loadBegin = () => ({ type: LIST_PAGE_LOAD_BEGIN });

export const loadSuccess = (filter, ui, data) => ({
    type: LIST_PAGE_LOAD_SUCCESS,
    payload: {
        filter,
        ui,
        data,
    },
});

export const loadFailure = error => ({
    type: LIST_PAGE_LOAD_FAILURE,
    payload: { error },
});

export const filterSet = (id, newValue) => ({
    type: LIST_PAGE_FILTER_SET,
    payload: {
        id,
        newValue,
    },
});

export const filterResetBegin = () => ({ type: LIST_PAGE_FILTER_RESET_BEGIN });

export const filterResetSuccess = filter => ({
    type: LIST_PAGE_FILTER_RESET_SUCCESS,
    payload: { filter },
});

export const loadList = () => (dispatch) => {
    dispatch(loadBegin());

    fetch(
        getServiceURL('books/initial-list'),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(json => dispatch(loadSuccess(json.filter, json.ui, json.dataList)))
        .catch(error => dispatch(loadFailure(error.message)));
};

export const setFilter = (id, newValue) => (dispatch) => {
    dispatch(filterSet(id, newValue));
};

export const resetFilter = () => (dispatch) => {
    dispatch(filterResetBegin());

    fetch(
        getServiceURL('books/filterReset'),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(json => dispatch(filterResetSuccess(json)))
        .catch(error => dispatch(loadFailure(error.message)));
};
