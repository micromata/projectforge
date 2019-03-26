import history from '../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const EDIT_PAGE_LOAD_BEGIN = 'EDIT_PAGE_LOAD_BEGIN';
export const EDIT_PAGE_LOAD_SUCCESS = 'EDIT_PAGE_LOAD_SUCCESS';
export const EDIT_PAGE_LOAD_FAILURE = 'EDIT_PAGE_LOAD_FAILURE';

export const EDIT_PAGE_FIELD_CHANGE = 'EDIT_PAGE_FIELD_CHANGE';

export const EDIT_PAGE_UPDATE_BEGIN = 'EDIT_PAGE_UPDATE_BEGIN';
export const EDIT_PAGE_UPDATE_FAILURE = 'EDIT_PAGE_UPDATE_FAILURE';

export const loadBegin = category => ({
    type: EDIT_PAGE_LOAD_BEGIN,
    payload: { category },
});

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

export const updateBegin = () => ({ type: EDIT_PAGE_UPDATE_BEGIN });
export const updateFailure = validationMessages => ({
    type: EDIT_PAGE_UPDATE_FAILURE,
    payload: { validationMessages },
});

export const loadEdit = (category, id) => (dispatch) => {
    dispatch(loadBegin(category));

    const params = {};

    if (id) {
        params.id = id;
    }

    return fetch(
        getServiceURL(`${category}/edit`, params),
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

const redirectToCategory = category => history.push(`/${category}/`);

export const updatePageData = () => (dispatch, getState) => {
    dispatch(updateBegin());

    const { values, category } = getState().editPage;

    fetch(
        getServiceURL(`${category}/saveorupdate`),
        {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ...values,
            }),
        },
    )
        .then((response) => {
            if (response.status === 200) {
                redirectToCategory(category);
                return;
            }

            if (response.status === 406) {
                response.json()
                    .then(json => dispatch(updateFailure(json.reduce((map, obj) => ({
                        ...map,
                        [obj['field-id']]: obj.message,
                    }), {}))));
                return;
            }

            throw new Error(response.statusText);
        })
        .catch(error => console.error(error));
};

export const changeField = (id, newValue) => dispatch => dispatch(fieldChanged(id, newValue));

export const abort = () => (dispatch, getState) => {
    const { category } = getState().editPage;

    redirectToCategory(category);
};
