import {
    MENU_LOAD_BEGIN,
    MENU_LOAD_FAILURE,
    MENU_LOAD_SUCCESS,
    USER_LOGIN_BEGIN,
} from '../actions';

const initialState = {
    loading: false,
    error: undefined,
    mainMenu: [],
    favoritesMenu: [],
    myAccountMenu: [],
};

const reducer = (state = initialState, { type, payload }) => {
    switch (type) {
        case USER_LOGIN_BEGIN:
        case MENU_LOAD_BEGIN:
            return {
                ...state,
                loading: true,
                error: undefined,
                mainMenu: [],
                favoritesMenu: [],
                myAccountMenu: [],
                badge: undefined,
            };
        case MENU_LOAD_SUCCESS:
            return {
                ...state,
                loading: false,
                badge: payload.mainMenu.badge,
                mainMenu: payload.mainMenu.menuItems,
                myAccountMenu: payload.myAccountMenu.menuItems,
                favoritesMenu: payload.favoritesMenu.menuItems,
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
