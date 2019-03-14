export const EDIT_FORM_FIELD_CHANGED = 'EDIT_FORM_FIELD_CHANGED';

const editFormChanged = (id, newValue) => ({
    type: EDIT_FORM_FIELD_CHANGED,
    payload: {
        id,
        newValue,
    },
});

export const changeField = (id, newValue) => dispatch => dispatch(editFormChanged(id, newValue));
