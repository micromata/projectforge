import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const EDIT_PAGE_LOAD_BEGIN = 'EDIT_PAGE_LOAD_BEGIN';
export const EDIT_PAGE_LOAD_SUCCESS = 'EDIT_PAGE_LOAD_SUCCESS';
export const EDIT_PAGE_LOAD_FAILURE = 'EDIT_PAGE_LOAD_FAILURE';

export const EDIT_PAGE_FIELD_CHANGE = 'EDIT_PAGE_FIELD_CHANGE';
export const EDIT_PAGE_ALL_FIELDS_SET = 'EDIT_PAGE_ALL_FIELDS_SET';

export const loadBegin = () => ({ type: EDIT_PAGE_LOAD_BEGIN });

export const loadSuccess = (data, ui) => ({
    type: EDIT_PAGE_LOAD_SUCCESS,
    payload: {
        data,
        ui,
    },
});

export const loadFailure = error => ({
    type: EDIT_PAGE_LOAD_FAILURE,
    payload: { error },
});

export const fieldChanged = (id, newValue) => ({
    type: EDIT_PAGE_FIELD_CHANGE,
    payload: {
        id,
        newValue,
    },
});

export const allFieldsSet = values => ({
    type: EDIT_PAGE_ALL_FIELDS_SET,
    payload: {
        values,
    },
});

export const loadEdit = id => (dispatch) => {
    dispatch(loadBegin());

    const params = {};

    if (id) {
        params.id = id;
    }

    return fetch(
        getServiceURL('books/edit', params),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(json => dispatch(loadSuccess(json.data, json.ui)))
        .catch(error => dispatch(loadFailure(error.message)));
};

export const updatePageData = () => (dispatch, getState) => {
    const { values } = getState().editPage;
    console.log(values);

    fetch(
        getServiceURL('books/saveorupdate'),
        {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ...values,
                // TODO: REMOVE DATE IGNORANCE
                created: undefined,
                lastUpdate: undefined,
            }),
        },
    )
        .then(handleHTTPErrors)
        // TODO: HANDLE FAILURE AND SUCCESS
        .then(response => console.log(response))
        .catch(error => console.error(error));
};

export const changeField = (id, newValue) => dispatch => dispatch(fieldChanged(id, newValue));

export const setAllFields = values => dispatch => dispatch(allFieldsSet(values));
