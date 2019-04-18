import { REST_ADD_TOAST } from '../actions';

const initialState = {
    toast: [],
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case REST_ADD_TOAST:
            console.log(payload);
            return state;
        default:
            return state;
    }
};
