import { EDIT_CALL_INITIAL_BEGIN, EDIT_SWITCH_CATEGORY } from '../../actions';

const initialState = null;

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case EDIT_CALL_INITIAL_BEGIN:
        case EDIT_SWITCH_CATEGORY:
            return payload.category;
        default:
            return state;
    }
};

export default reducer;
