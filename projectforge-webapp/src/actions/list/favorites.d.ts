// Action Types
export const LIST_FAVORITES_RECEIVED: 'LIST_FAVORITES_RECEIVED';

// State interfaces
interface ListState {
    currentCategory: string;
    categories: {
        [key: string]: {
            filter: FilterState;
        };
    };
}

interface FilterState {
    [key: string]: any; // Replace with actual filter state structure
}

// Action Interfaces
interface ListFavoritesReceivedAction {
    type: typeof LIST_FAVORITES_RECEIVED;
    payload: {
        category: string;
        response: any; // Replace with actual response structure
    };
}

// Parameters interfaces
interface CreateFavoriteParams {
    name: string;
    [key: string]: any; // Additional parameters
}

interface DeleteFavoriteParams {
    id: string | number;
}

interface RenameFavoriteParams {
    id: string | number;
    newName: string;
}

interface SelectFavoriteParams {
    id: string | number;
}

// Action Creator Types
type GetState = () => { list: ListState };
type Dispatch = (action: any) => any;
type ThunkAction<R = Promise<any>> = (dispatch: Dispatch, getState: GetState) => R;

// Action Creators
export function createFavorite(body: CreateFavoriteParams): ThunkAction;

export function deleteFavorite(params: DeleteFavoriteParams): ThunkAction;

export function renameFavorite(params: RenameFavoriteParams): ThunkAction;

export function selectFavorite(params: SelectFavoriteParams): ThunkAction;

export function updateFavorite(): ThunkAction;
