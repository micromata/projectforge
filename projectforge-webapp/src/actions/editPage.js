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

export const changeField = (id, newValue) => dispatch => dispatch(fieldChanged(id, newValue));

export const setAllFields = values => dispatch => dispatch(allFieldsSet(values));
