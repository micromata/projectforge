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

export const fetchFavorites = (action, { params = {}, body }) => (dispatch, getState) => {
    const category = getState().list.currentCategory;

    return fetch(
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
        .then(response => response.json())
        .then(response => dispatch(saveFavoritesResponse(category, response)))
        .catch(error => dispatch(fetchFailure(error)));
};
