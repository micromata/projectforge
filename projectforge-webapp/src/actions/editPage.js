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

export const loadSuccess = (data, ui, variables, onClose) => ({
    type: EDIT_PAGE_LOAD_SUCCESS,
    payload: {
        data,
        ui,
        variables,
        onClose,
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

export const loadEdit = (category, id, additionalParams, onClose) => (dispatch) => {
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
        .then(json => dispatch(loadSuccess(json.data, json.ui, json.variables, onClose)))
        .catch(error => dispatch(loadFailure(error.message)));
};

// preserveObject avoids the replacement of the object by its id. Default is undefined.
export const changeField = (id, newValue) => (dispatch) => {
    if (typeof newValue === 'object' && newValue) {
        if (newValue.id) {
            // For objects such as PFUserDO, TaskDO only needs the id (pk).
            return dispatch(fieldChanged(id, { id: newValue.id }));
        }
        if (newValue.value) { // For Select boxes.
            return dispatch(fieldChanged(id, newValue.value));
        }
    }
    return dispatch(fieldChanged(id, newValue));
};

export const callEndpointWithData = (endpoint, method = 'POST') => (dispatch, getState) => {
    dispatch(updateBegin());

    const { category, data, onClose } = getState().editPage;

    return fetch(
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
        .then((response) => {
            if (response.status === 200) {
                return response.json()
                    .then((json) => {
                        history.push(json.url);
                        if (onClose) {
                            onClose(json);
                        }
                    });
            }

            if (response.status === 406) {
                return response.json()
                    .then(({ validationErrors }) => dispatch(
                        updateFailure(validationErrors.reduce((map, obj) => ({
                            ...map,
                            [obj.fieldId]: obj.message,
                        }), {})),
                    ));
            }

            throw new Error(response.status);
        })
        .catch(error => dispatch(loadFailure(error)));
};

export const updatePageData = () => callEndpointWithData('saveorupdate', 'PUT');

export const abort = () => callEndpointWithData('cancel');

export const markAsDeleted = () => callEndpointWithData('markAsDeleted', 'DELETE');

export const undelete = () => callEndpointWithData('undelete', 'PUT');

export const clone = () => callEndpointWithData('clone');
