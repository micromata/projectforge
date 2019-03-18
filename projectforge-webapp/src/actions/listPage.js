import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const LIST_PAGE_LOAD_BEGIN = 'EDIT_PAGE_LOAD_BEGIN';
export const LIST_PAGE_LOAD_SUCCESS = 'EDIT_PAGE_LOAD_SUCCESS';
export const LIST_PAGE_LOAD_FAILURE = 'EDIT_PAGE_LOAD_FAILURE';

export const loadBegin = () => ({ type: LIST_PAGE_LOAD_BEGIN });

export const loadSuccess = (ui, data) => ({
    type: LIST_PAGE_LOAD_SUCCESS,
    payload: {
        ui,
        data,
    },
});

export const loadFailure = error => ({
    type: LIST_PAGE_LOAD_FAILURE,
    payload: { error },
});

export const loadList = () => (dispatch) => {
    dispatch(loadBegin());

    fetch(
        getServiceURL('books/list'),
        {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({}),
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json)
        .then(json => console.log(json))
        .then(error => console.log(error));
};
