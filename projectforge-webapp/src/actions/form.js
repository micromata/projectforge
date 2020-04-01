import history from '../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';
import { loadUserStatus } from './authentication';
import { addToast } from './toast';

export const FORM_CALL_ACTION_BEGIN = 'FORM_CALL_ACTION_BEGIN';
export const FORM_CALL_ACTION_SUCCESS = 'FORM_CALL_ACTION_SUCCESS';
export const FORM_CALL_FAILURE = 'FORM_CALL_FAILURE';
export const FORM_CALL_INITIAL_BEGIN = 'FORM_CALL_INITIAL_BEGIN';
export const FORM_CALL_SUCCESS = 'FORM_CALL_SUCCESS';
export const FORM_CHANGE_DATA = 'FORM_CHANGE_DATA';
export const FORM_CHANGE_VARIABLES = 'FORM_CHANGE_VARIABLES';
export const FORM_SWITCH_CATEGORY = 'FORM_SWITCH_CATEGORY';

const callActionBegin = category => ({
    type: FORM_CALL_ACTION_BEGIN,
    payload: { category },
});

const callActionSuccess = category => ({
    type: FORM_CALL_ACTION_SUCCESS,
    payload: { category },
});

const callFailure = (category, error) => ({
    type: FORM_CALL_FAILURE,
    payload: {
        category,
        error,
    },
});

const callInitialBegin = (category, id) => ({
    type: FORM_CALL_INITIAL_BEGIN,
    payload: {
        category,
        id,
    },
});

const callSuccess = (category, response) => ({
    type: FORM_CALL_SUCCESS,
    payload: {
        category,
        response,
    },
});

const changeData = (category, newData) => ({
    type: FORM_CHANGE_DATA,
    payload: {
        category,
        newData,
    },
});

const changeVariables = (category, newVariables) => ({
    type: FORM_CHANGE_VARIABLES,
    payload: {
        category,
        newVariables,
    },
});

const switchCategoryWithData = (from, to, newVariables) => ({
    type: FORM_SWITCH_CATEGORY,
    payload: {
        from,
        newVariables,
        category: to,
    },
});

export const loadFormPage = (category, id, url, params = {}) => (dispatch, getState) => {
    const currentCategory = getState().form.categories[category];

    if (currentCategory && currentCategory.isFetching) {
        return Promise.resolve();
    }

    dispatch(callInitialBegin(category, id));

    return fetch(
        url,
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(json => dispatch(callSuccess(category, Object.combine(params, json))))
        .catch(error => callFailure(category, error));
};

export const callAction = (
    {
        responseAction: action,
        watchFieldsTriggered,
    },
) => (dispatch, getState) => {
    if (!action) {
        return Promise.reject(Error('No response action given.'));
    }

    const { form: state } = getState();
    const category = state.currentCategory;

    if (action.message) {
        addToast(action.message.message, action.message.color)(dispatch);
    }

    switch (action.targetType) {
        case 'REDIRECT':
        case 'MODAL': {
            const historyState = { serverData: action.variables };

            if (action.targetType === 'MODAL') {
                historyState.background = history.location;
            }

            history.push(action.url, historyState);
            break;
        }
        case 'CLOSE_MODAL':
            if (history.location.state && history.location.state.background) {
                history.push(history.location.state.background);
            }
            break;
        case 'UPDATE':
            if (action.url) {
                history.push(
                    `${action.url}`,
                    {
                        noReload: true,
                        newVariables: action.variables,
                    },
                );
                window.scrollTo(0, 0);
            } else {
                dispatch(callSuccess(category, action.variables));
            }
            break;
        case 'CHECK_AUTHENTICATION':
            return loadUserStatus()(dispatch)
                .then(() => {
                    if (action.url) {
                        history.push(action.url);
                    }
                });
        case 'NOTHING':
            break;
        case 'DELETE':
        case 'POST':
        case 'GET':
        case 'PUT': {
            dispatch(callActionBegin(category));

            let status = 0;

            const { data, serverData } = state.categories[category];

            return fetch(
                getServiceURL(action.url),
                {
                    method: action.targetType,
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        data,
                        watchFieldsTriggered,
                        serverData,
                    }),
                },
            )
                .then((response) => {
                    ({ status } = response);

                    if (response.headers.get('Content-Type')
                        .includes('application/json')) {
                        return response.json();
                    }

                    throw Error(`Error ${status}`);
                })
                .then((json) => {
                    dispatch(callActionSuccess(category));

                    if (status === 406) {
                        dispatch(callSuccess(
                            category,
                            { validationErrors: json.validationErrors },
                        ));
                        window.scrollTo(0, 0);
                        return Promise.resolve();
                    }

                    return callAction({ responseAction: json })(dispatch, getState);
                })
                .catch(error => dispatch(callFailure(category, error)));
        }
        default:
            return Promise.reject(Error(`TargetType ${action.targetType} not implemented.`));
    }
    return Promise.resolve();
};

export const setCurrentData = newData => (dispatch, getState) => {
    const { form } = getState();
    const { categories, currentCategory } = form;
    const { ui } = categories[currentCategory];

    // Change Data in redux model
    dispatch(changeData(form.currentCategory, Object.convertPathKeys(newData)));

    // Check for triggered watch fields
    if (ui.watchFields) {
        const watchFieldsTriggered = Object.keys(newData)
            .filter(key => ui.watchFields.includes(key));

        if (watchFieldsTriggered.length > 0) {
            callAction({
                responseAction: {
                    url: `${currentCategory}/watchFields`,
                    targetType: 'POST',
                },
                watchFieldsTriggered,
            })(dispatch, getState);
        }
    }
};

export const setCurrentVariables = newVariables => (dispatch, getState) => dispatch(
    changeVariables(getState().form.currentCategory, newVariables),
);

export const switchFromCurrentCategory = (to, newVariables) => (dispatch, getState) => {
    const { form: state } = getState();
    const from = state.currentCategory;

    dispatch(switchCategoryWithData(
        from,
        to,
        {
            ...state[from],
            ...newVariables,
        },
    ));
};
