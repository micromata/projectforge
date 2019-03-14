import { EDIT_FORM_FIELD_CHANGED } from '../actions';

const initialState = {
    values: {},
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case EDIT_FORM_FIELD_CHANGED:
            return {
                ...state,
                values: {
                    ...state.values,
                    [payload.id]: payload.newValue,
                },
            };
        default:
            return state;
    }
};

export default reducer;
