import { EDIT_PAGE_ALL_FIELDS_SET, EDIT_PAGE_FIELD_CHANGE } from '../actions';

const initialState = {
    values: {},
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case EDIT_PAGE_FIELD_CHANGE:
            return {
                ...state,
                values: {
                    ...state.values,
                    [payload.id]: payload.newValue,
                },
            };
        case EDIT_PAGE_ALL_FIELDS_SET:
            return {
                ...state,
                values: payload.values,
            };
        default:
            return state;
    }
};

export default reducer;
