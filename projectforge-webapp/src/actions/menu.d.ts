// Action Types
export const MENU_LOAD_BEGIN: 'MENU_LOAD_BEGIN';
export const MENU_LOAD_SUCCESS: 'MENU_LOAD_SUCCESS';
export const MENU_LOAD_FAILURE: 'MENU_LOAD_FAILURE';

// Menu Item interfaces
interface MenuItem {
    [key: string]: any; // Replace with specific menu item structure
}

// Action Interfaces
interface MenuLoadBeginAction {
    type: typeof MENU_LOAD_BEGIN;
}

interface MenuLoadSuccessAction {
    type: typeof MENU_LOAD_SUCCESS;
    payload: {
        favoritesMenu: MenuItem[];
        mainMenu: MenuItem[];
        myAccountMenu: MenuItem[];
    };
}

interface MenuLoadFailureAction {
    type: typeof MENU_LOAD_FAILURE;
    payload: {
        error: Error;
    };
}

// Union type for all possible actions
export type MenuActionTypes =
    | MenuLoadBeginAction
    | MenuLoadSuccessAction
    | MenuLoadFailureAction;

// Action Creator Types
export type AppDispatch = <T extends MenuActionTypes>(action: T) => T;
export type ThunkAction = (dispatch: AppDispatch) => Promise<void>;

// Action Creators
export function loadBegin(): MenuLoadBeginAction;

export function loadSuccess(
    favoritesMenu: MenuItem[],
    mainMenu: MenuItem[],
    myAccountMenu: MenuItem[]
): MenuLoadSuccessAction;

export function loadFailure(error: Error): MenuLoadFailureAction;

// Thunk Action Creator
export function loadMenu(): ThunkAction;
