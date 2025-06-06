// Action Types
export const TOAST_ADD: 'TOAST_ADD';
export const TOAST_REMOVE: 'TOAST_REMOVE';
export const TOAST_CLEAR_ALL: 'TOAST_CLEAR_ALL';

// Action Interfaces
interface ToastAddAction {
    type: typeof TOAST_ADD;
    payload: {
        id: string;
        message: string;
        color?: string;
    };
}

interface ToastRemoveAction {
    type: typeof TOAST_REMOVE;
    payload: {
        id: string;
    };
}

interface ToastClearAllAction {
    type: typeof TOAST_CLEAR_ALL;
}

// Union type for all possible actions
export type ToastActionTypes =
    | ToastAddAction
    | ToastRemoveAction
    | ToastClearAllAction;

// Action Creator Types
export type AppDispatch = <T extends ToastActionTypes>(action: T) => T;
export type ThunkAction = (dispatch: AppDispatch) => void;

// Action Creators
export function removeToast(id: string): ToastRemoveAction;

export function clearAllToasts(): ToastClearAllAction;

export function addToast(
    message: string,
    color?: string,
    stay?: boolean
): ThunkAction;
