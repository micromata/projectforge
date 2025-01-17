// Action Types
export const LIST_FILTER_ADD: 'LIST_FILTER_ADD';
export const LIST_FILTER_REMOVE: 'LIST_FILTER_REMOVE';
export const LIST_FILTER_RESET: 'LIST_FILTER_RESET';
export const LIST_FILTER_SEARCH_STRING_CHANGED: 'LIST_FILTER_SEARCH_STRING_CHANGED';
export const LIST_FILTER_SET: 'LIST_FILTER_SET';
export const LIST_FILTER_SORT: 'LIST_FILTER_SORT';

// Sort Order Type
type SortOrder = 'ASCENDING' | 'DESCENDING';

// Sort Property Interface
interface SortProperty {
    property: string;
    sortOrder: SortOrder;
}

// Action Interfaces
interface ListFilterAddAction {
    type: typeof LIST_FILTER_ADD;
    payload: {
        category: string;
        fieldId: string;
    };
}

interface ListFilterRemoveAction {
    type: typeof LIST_FILTER_REMOVE;
    payload: {
        category: string;
        fieldId: string;
    };
}

interface ListFilterResetAction {
    type: typeof LIST_FILTER_RESET;
    payload: {
        category: string;
    };
}

interface ListFilterSearchStringChangedAction {
    type: typeof LIST_FILTER_SEARCH_STRING_CHANGED;
    payload: {
        category: string;
        searchString: string;
    };
}

interface ListFilterSetAction {
    type: typeof LIST_FILTER_SET;
    payload: {
        category: string;
        fieldId: string;
        newValue: any; // Replace with more specific type if possible
    };
}

interface ListFilterSortAction {
    type: typeof LIST_FILTER_SORT;
    payload: {
        category: string;
        column: string;
        sortProperty: SortProperty | undefined;
    };
}

// Union type for all possible actions
export type ListFilterActionTypes =
    | ListFilterAddAction
    | ListFilterRemoveAction
    | ListFilterResetAction
    | ListFilterSearchStringChangedAction
    | ListFilterSetAction
    | ListFilterSortAction;

// State Type
interface ListState {
    currentCategory: string;
    // Add other state properties as needed
}

// Action Creator Types
type GetState = () => { list: ListState };
type Dispatch = (action: any) => any;
type ThunkAction = (dispatch: Dispatch, getState: GetState) => void;

// Exported Action Creators
export function addFilter(fieldId: string): ThunkAction;

export function removeFilter(fieldId: string): ThunkAction;

export function changeSearchString(searchString: string): ThunkAction;

export function resetAllFilters(): ThunkAction;

export function setFilter(fieldId: string, newValue: any): ThunkAction;

export function sortList(
    property: string,
    sortOrder?: SortOrder
): ThunkAction;
