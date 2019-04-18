import { getServiceURL, handleHTTPErrors } from '../utilities/rest';

export const REST_ADD_TOAST = 'REST_ADD_TOAST';

export const addToast = (message, style) => ({
    type: REST_ADD_TOAST,
    payload: {
        message,
        style,
    },
});

export const performGetCall = url => dispatch => fetch(
    getServiceURL(url),
    {
        method: 'GET',
        credentials: 'include',
    },
)
    .then(handleHTTPErrors)
    .then(response => response.json())
    .then(({ messageType, message, style }) => {
        if (messageType === 'TOAST') {
            dispatch(addToast(message, style));
        } else {
            throw Error(`Message Type ${messageType} not found.`);
        }
    })
    .catch(error => dispatch(addToast(error, 'danger')));
