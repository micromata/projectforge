import { EDIT_CALL_INITIAL_BEGIN } from '../../actions';

const initialState = null;

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case EDIT_CALL_INITIAL_BEGIN:
            return payload.category;
        default:
            return state;
    }
};

export default reducer;
