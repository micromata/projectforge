import { MENU_LOAD_BEGIN, MENU_LOAD_FAILURE, MENU_LOAD_SUCCESS } from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    categories: [],
    favorites: [],
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case MENU_LOAD_BEGIN:
            return {
                ...state,
                loading: true,
                error: undefined,
                categories: [],
                favorites: [],
            };
        case MENU_LOAD_SUCCESS:
            return {
                ...state,
                loading: false,
                categories: payload.mainMenu.menuItems,
                badge: payload.mainMenu.badge,
                favorites: payload.favoritesMenu.menuItems,
            };
        case MENU_LOAD_FAILURE:
            return {
                ...state,
                loading: false,
                error: payload.error,
            };
        default:
            return state;
    }
};

export default reducer;
