import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const LIST_PAGE_LOAD_BEGIN = 'LIST_PAGE_LOAD_BEGIN';
export const LIST_PAGE_LOAD_SUCCESS = 'LIST_PAGE_LOAD_SUCCESS';
export const LIST_PAGE_LOAD_FAILURE = 'LIST_PAGE_LOAD_FAILURE';

export const LIST_PAGE_FILTER_SET = 'LIST_PAGE_FILTER_SET';

export const LIST_PAGE_FILTER_RESET_BEGIN = 'LIST_PAGE_FILTER_RESET_BEGIN';
export const LIST_PAGE_FILTER_RESET_SUCCESS = 'LIST_PAGE_FILTER_RESET_SUCCESS';

export const LIST_PAGE_DATA_UPDATE_BEGIN = 'LIST_PAGE_DATA_UPDATE_BEGIN';
export const LIST_PAGE_DATA_UPDATE_SUCCESS = 'LIST_PAGE_DATA_UPDATE_SUCCESS';

export const loadBegin = category => ({
    type: LIST_PAGE_LOAD_BEGIN,
    payload: { category },
});

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

export const dataUpdateBegin = () => ({ type: LIST_PAGE_DATA_UPDATE_BEGIN });

export const dataUpdateSuccess = data => ({
    type: LIST_PAGE_DATA_UPDATE_SUCCESS,
    payload: { data },
});

export const loadList = category => (dispatch) => {
    dispatch(loadBegin(category));

    return fetch(
        getServiceURL(`${category}/initial-list`),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(json => dispatch(loadSuccess(json.filter, json.ui, json.data)))
        .catch(({ message }) => dispatch(loadFailure(message)));
};

export const setFilter = (id, newValue) => (dispatch) => {
    dispatch(filterSet(id, newValue));
};

export const resetFilter = () => (dispatch, getState) => {
    const { category } = getState().listPage;

    dispatch(filterResetBegin());

    return fetch(
        getServiceURL(`${category}/filterReset`),
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

export const updateData = () => (dispatch, getState) => {
    const { category, filter } = getState().listPage;
    dispatch(dataUpdateBegin());

    return fetch(
        getServiceURL(`${category}/list`),
        {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(filter),
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(json => dispatch(dataUpdateSuccess(json)))
        .catch(error => dispatch(loadFailure(error.message)));
};
