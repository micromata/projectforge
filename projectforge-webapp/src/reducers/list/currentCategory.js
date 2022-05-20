import { LIST_SWITCH_CATEGORY } from '../../actions';

const initialState = null;

const reducer = (state = initialState, { type, payload } = {}) => {
    switch (type) {
        case LIST_SWITCH_CATEGORY:
            return payload.category;
        default:
            return state;
    }
};

export default reducer;
