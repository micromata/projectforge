import { getServiceURL, handleHTTPErrors } from '../../utilities/rest';
import { fetchFailure } from './index';

export const LIST_FAVORITES_RECEIVED = 'LIST_FAVORITES_RECEIVED';

const saveFavoritesResponse = (category, response) => ({
    type: LIST_FAVORITES_RECEIVED,
    payload: {
        category,
        response,
    },
});
const getCurrentCategory = (state) => state.list.currentCategory;
const getCurrentFilter = (state) => state.list.categories[getCurrentCategory(state)].filter;

const fetchFavorites = (action, { params = {}, body }, category, dispatch) => fetch(
    getServiceURL(`${category}/filter/${action}`, params),
    {
        method: body ? 'POST' : 'GET',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
        },
        body: JSON.stringify(body),
    },
)
    .then(handleHTTPErrors)
    .then((response) => response.json())
    .then((response) => dispatch(saveFavoritesResponse(category, response)))
    .catch((error) => dispatch(fetchFailure(category, error.message)));

/**
 * Dispatch a call to create a new favorite with current filter values.
 *
 * @param body Body for the POST call.
 * @param body.name Name of the new filter.
 * @returns {function(...[*]=)}
 */
export const createFavorite = (body) => (dispatch, getState) => {
    const state = getState();

    return fetchFavorites(
        'create',
        {
            body: {
                ...getCurrentFilter(state),
                ...body,
            },
        },
        getCurrentCategory(state),
        dispatch,
    );
};

/**
 * Dispatch a call to delete a favorite
 *
 * @param params Parameters for the GET call.
 * @param params.id The id of the filter to delete.
 * @returns {function(*=, *): *}
 */
export const deleteFavorite = (params) => (dispatch, getState) => fetchFavorites(
    'delete',
    { params },
    getCurrentCategory(getState()),
    dispatch,
);

/**
 * Dispatch a call to rename a favorite.
 *
 * @param params Params for the GET call.
 * @param params.id The id of the favorite to rename.
 * @param params.newName The new name of the favorite.
 * @returns {function(*=, *): *}
 */
export const renameFavorite = (params) => (dispatch, getState) => fetchFavorites(
    'rename',
    { params },
    getCurrentCategory(getState()),
    dispatch,
);

/**
 * Dispatch a call to select a favorite.
 *
 * @param params Params for the GET call.
 * @param params.id The id of the favorite to select.
 * @returns {function(*=, *): *}
 */
export const selectFavorite = (params) => (dispatch, getState) => fetchFavorites(
    'select',
    { params },
    getCurrentCategory(getState()),
    dispatch,
);

/**
 * Dispatch a call to update the current favorite.
 *
 * @returns {function(...[*]=)}
 */
export const updateFavorite = () => (dispatch, getState) => {
    const state = getState();

    return fetchFavorites(
        'update',
        { body: getCurrentFilter(state) },
        getCurrentCategory(state),
        dispatch,
    );
};
