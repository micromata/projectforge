export const EDIT_PAGE_FIELD_CHANGED = 'EDIT_PAGE_FIELD_CHANGED';
export const EDIT_PAGE_ALL_FIELDS_SET = 'EDIT_PAGE_ALL_FIELDS_SET';

const fieldChanged = (id, newValue) => ({
    type: EDIT_PAGE_FIELD_CHANGED,
    payload: {
        id,
        newValue,
    },
});

const allFieldsSet = values => ({
    type: EDIT_PAGE_ALL_FIELDS_SET,
    payload: {
        values,
    },
});

export const changeField = (id, newValue) => dispatch => dispatch(fieldChanged(id, newValue));

export const setAllFields = values => dispatch => dispatch(allFieldsSet(values));
