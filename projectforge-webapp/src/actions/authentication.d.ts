// Action Types
export const USER_LOGIN_BEGIN = 'USER_LOGIN_BEGIN';
export const USER_LOGIN_SUCCESS = 'USER_LOGIN_SUCCESS';
export const USER_LOGIN_FAILURE = 'USER_LOGIN_FAILURE';

// Interfaces for the data structures
interface UserData {
    // Add user data properties based on your needs
    [key: string]: any;
}

interface SystemData {
    version: string;
    buildTimestamp: string;
}

// Action Interfaces
interface UserLoginBeginAction {
    type: typeof USER_LOGIN_BEGIN;
}

interface UserLoginSuccessAction {
    type: typeof USER_LOGIN_SUCCESS;
    payload: {
        user: UserData;
        version: string;
        buildTimestamp: string;
        alertMessage?: string;
    };
}

interface UserLoginFailureAction {
    type: typeof USER_LOGIN_FAILURE;
    payload: {
        error: string;
    };
}

// Union type for all possible actions
export type UserActionTypes =
    | UserLoginBeginAction
    | UserLoginSuccessAction
    | UserLoginFailureAction;

// Action Creator Types
export type AppDispatch = <T extends UserActionTypes>(action: T) => T;
export type ThunkAction = (dispatch: AppDispatch) => Promise<void>;

// Action Creator Functions
export const userLoginBegin: () => UserLoginBeginAction;
export const userLoginSuccess: (
    user: UserData,
    version: string,
    buildTimestamp: string,
    alertMessage?: string
) => UserLoginSuccessAction;
export const userLoginFailure: (error: string) => UserLoginFailureAction;

// Thunk Action Creators
export const loadUserStatus: () => ThunkAction;
export const login: (
    username: string,
    password: string,
    keepSignedIn: boolean
) => ThunkAction;
