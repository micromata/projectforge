import revisedRandomId from '../utilities/revisedRandomId';

export const TOAST_ADD = 'TOAST_ADD';
export const TOAST_REMOVE = 'TOAST_REMOVE';
export const TOAST_CLEAR_ALL = 'TOAST_CLEAR_ALL';

const add = (id, message, color) => ({
    type: TOAST_ADD,
    payload: {
        id,
        message,
        color,
    },
});

export const removeToast = (id) => ({
    type: TOAST_REMOVE,
    payload: { id },
});

export const clearAllToasts = () => ({
    type: TOAST_CLEAR_ALL,
});

export const addToast = (
    message,
    color = undefined,
    stay = true,
) => (dispatch) => {
    const id = revisedRandomId();

    dispatch(add(id, message, color));

    if (stay === false) {
        window.setTimeout(() => dispatch(removeToast(id)), 10 * 1000);
    }
};
