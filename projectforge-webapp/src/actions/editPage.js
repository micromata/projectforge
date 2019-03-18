import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const EDIT_PAGE_FIELD_CHANGE = 'EDIT_PAGE_FIELD_CHANGE';
export const EDIT_PAGE_ALL_FIELDS_SET = 'EDIT_PAGE_ALL_FIELDS_SET';

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
