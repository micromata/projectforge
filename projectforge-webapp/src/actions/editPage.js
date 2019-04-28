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

export const loadSuccess = (data, ui, variables) => ({
    type: EDIT_PAGE_LOAD_SUCCESS,
    payload: {
        data,
        ui,
        variables,
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

export const loadEdit = (category, id, additionalParams) => (dispatch) => {
    dispatch(loadBegin(category));

    const params = {
        ...additionalParams,
    };

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
        .then(json => dispatch(loadSuccess(json.data, json.ui, json.variables)))
        .catch(error => dispatch(loadFailure(error.message)));
};

export const updatePageData = () => (dispatch, getState) => {
    dispatch(updateBegin());

    const { data, category } = getState().editPage;

    fetch(
        getServiceURL(`${category}/saveorupdate`),
        {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ...data,
            }),
        },
    )
        .then((response) => {
            if (response.status === 200) {
                response.json()
                    .then((json) => {
                        history.push(json.url);
                    });
            }

            if (response.status === 406) {
                response.json()
                    .then(json => dispatch(updateFailure(json.reduce((map, obj) => ({
                        ...map,
                        [obj.fieldId]: obj.message,
                    }), {}))));
                return;
            }

            throw new Error(response.status);
        })
        .catch(error => dispatch(loadFailure(error)));
};

// preserveObject avoids the replacement of the object by its id. Default is undefined.
export const changeField = (id, newValue, preserveObject) => (dispatch) => {
    if (!preserveObject && typeof newValue === 'object' && newValue.id) {
        // For objects such as PFUserDO, TaskDO only the id (pk) is needed to provide to the server.
        return dispatch(fieldChanged(id, { id: newValue.id }));
    }
    return dispatch(fieldChanged(id, newValue));
};

export const callEndpointWithData = (category, endpoint, data, dispatch, method = 'POST') => {
    dispatch(updateBegin());

    fetch(
        getServiceURL(`${category}/${endpoint}`),
        {
            method,
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        },
    )
        .then(handleHTTPErrors)
        .then((response) => {
            if (response.status === 200) {
                response.json()
                    .then((json) => {
                        history.push(json.url);
                    });
            }

            if (response.status === 406) {
                response.json()
                    .then(json => dispatch(updateFailure(json.reduce((map, obj) => ({
                        ...map,
                        [obj.fieldId]: obj.message,
                    }), {}))));
                return;
            }

            throw new Error(response.status);
        })
        .catch(error => dispatch(loadFailure(error)));
};

export const abort = () => (dispatch, getState) => {
    const { category, data } = getState().editPage;

    callEndpointWithData(category, 'cancel', data, dispatch, 'POST');
};


export const markAsDeleted = () => (dispatch, getState) => {
    const { category, data } = getState().editPage;

    callEndpointWithData(category, 'markAsDeleted', data, dispatch, 'DELETE');
};

export const undelete = () => (dispatch, getState) => {
    const { category, data } = getState().editPage;

    callEndpointWithData(category, 'undelete', data, dispatch, 'PUT');
};

export const clone = () => (dispatch, getState) => {
    const { category, data } = getState().editPage;

    callEndpointWithData(category, 'clone', data, dispatch);
};
