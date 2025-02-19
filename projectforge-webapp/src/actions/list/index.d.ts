// Action Types
export const LIST_DISMISS_ERROR: 'LIST_DISMISS_ERROR';
export const LIST_SWITCH_CATEGORY: 'LIST_SWITCH_CATEGORY';
export const LIST_FETCH_FAILURE: 'LIST_FETCH_FAILURE';
export const LIST_INITIAL_CALL_BEGIN: 'LIST_INITIAL_CALL_BEGIN';
export const LIST_FETCH_DATA_BEGIN: 'LIST_FETCH_DATA_BEGIN';
export const LIST_CALL_SUCCESS: 'LIST_CALL_SUCCESS';

// State interfaces
interface CategoryState {
    isFetching: boolean;
    filter: Record<string, any>;
    lastQueriedFilter?: string;
    search?: string;
    standardEditPage?: string;
    data?: any;
}

interface ListState {
    currentCategory: string;
    categories: {
        [key: string]: CategoryState;
    };
}

// Action Interfaces
interface ListDismissErrorAction {
    type: typeof LIST_DISMISS_ERROR;
    payload: {
        category: string;
    };
}

interface ListSwitchCategoryAction {
    type: typeof LIST_SWITCH_CATEGORY;
    payload: {
        category: string;
    };
}

interface ListFetchFailureAction {
    type: typeof LIST_FETCH_FAILURE;
    payload: {
        category: string;
        error: string;
    };
}

interface ListInitialCallBeginAction {
    type: typeof LIST_INITIAL_CALL_BEGIN;
    payload: {
        category: string;
        search: string;
    };
}

interface ListFetchDataBeginAction {
    type: typeof LIST_FETCH_DATA_BEGIN;
    payload: {
        category: string;
        variables?: any;
    };
}

interface ListCallSuccessAction {
    type: typeof LIST_CALL_SUCCESS;
    payload: {
        category: string;
        response: {
            data?: any;
            targetType?: 'REDIRECT';
            url?: string;
        };
    };
}

// Union type for all possible actions
export type ListActionTypes =
    | ListDismissErrorAction
    | ListSwitchCategoryAction
    | ListFetchFailureAction
    | ListInitialCallBeginAction
    | ListFetchDataBeginAction
    | ListCallSuccessAction;

// Action Creator Types
type GetState = () => { list: ListState };
type Dispatch = (action: any) => any;
type ThunkAction<R = Promise<void>> = (dispatch: Dispatch, getState: GetState) => R;

// Exported Functions
export function fetchFailure(
    category: string,
    error: string
): ListFetchFailureAction;

export function dismissCurrentError(): ThunkAction;

export function loadList(
    category: string,
    ignoreLastQueriedFilters?: boolean,
    variables?: any
): ThunkAction;

export function fetchCurrentList(
    ignoreLastQueriedFilters?: boolean
): ThunkAction;

export function exportCurrentList(): ThunkAction;

export function startMultiSelection(): ThunkAction;

export function openEditPage(
    id: string | number
): ThunkAction<void>;
