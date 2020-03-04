import { FORM_CALL_INITIAL_BEGIN, FORM_SWITCH_CATEGORY } from '../../actions';

const initialState = null;

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case FORM_CALL_INITIAL_BEGIN:
        case FORM_SWITCH_CATEGORY:
            return payload.category || null;
        default:
            return state;
    }
};

export default reducer;
