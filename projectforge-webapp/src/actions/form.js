import fileDownload from 'js-file-download';
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

const callActionBegin = (category) => ({
    type: FORM_CALL_ACTION_BEGIN,
    payload: { category },
});

const callActionSuccess = (category) => ({
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

const mergeVariables = (categoryState, newVariables, merge) => {
    let variables;

    if (categoryState && newVariables && merge) {
        variables = Object.combine(categoryState, newVariables);
    } else {
        variables = newVariables || categoryState;
    }

    return variables;
};

export const switchFromCurrentCategory = (
    to,
    newVariables,
    merge = false,
) => (dispatch, getState) => {
    const { form: state } = getState();

    dispatch(switchCategoryWithData(
        state.currentCategory,
        to,
        mergeVariables(state.categories[to], newVariables, merge),
    ));
};

export const callAction = (
    {
        responseAction: action,
        watchFieldsTriggered,
    },
) => (dispatch, getState) => {
    if (!action) {
        return Promise.resolve();
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
                const backgroundCategory = history.location.state.background.pathname.split('/')[2];

                history.push(
                    history.location.state.background,
                    {
                        ...history.location.state.background.state,
                        noReload: true,
                        newVariables: action.variables,
                        merge: action.merge,
                    },
                );

                // switch to category to make category reactive again
                switchFromCurrentCategory(
                    backgroundCategory,
                    action.variables || {},
                    true,
                )(dispatch, getState);
            } else if (history.location.pathname.startsWith('/react/calendar')) {
                history.push('/react/calendar');
            }
            break;
        case 'UPDATE':
            if (action.url) {
                history.push(
                    `${action.url}`,
                    {
                        noReload: true,
                        newVariables: action.variables,
                        merge: action.merge,
                    },
                );
                window.scrollTo(0, 0);
            } else {
                dispatch(callSuccess(
                    category,
                    mergeVariables(state.categories[category], action.variables, action.merge),
                ));
            }
            break;
        case 'CHECK_AUTHENTICATION':
            return loadUserStatus()(dispatch)
                .then(() => {
                    if (action.url) {
                        history.push(action.url);
                    }
                });
        case 'DOWNLOAD': {
            let { url } = action;

            if (!action.absolute) {
                url = getServiceURL(url);
            }

            window.open(url, '_blank');
            return Promise.resolve();
        }
        case 'NOTHING':
        case 'TOAST':
            break;
        case 'DELETE':
        case 'POST':
        case 'GET':
        case 'PUT': {
            dispatch(callActionBegin(category));

            let status = 0;

            const { data, serverData } = state.categories[category];

            let filename;
            let body;
            const { myData } = action;

            if (action.targetType !== 'GET') {
                body = JSON.stringify({
                    data: myData || data,
                    watchFieldsTriggered,
                    serverData,
                });
            }

            return fetch(
                getServiceURL(action.url),
                {
                    method: action.targetType,
                    credentials: 'include',
                    headers: { 'Content-Type': 'application/json' },
                    body,
                },
            )
                .then((response) => {
                    ({ status } = response);

                    if (response.headers.get('Content-Type')
                        .includes('application/json')) {
                        return response.json();
                    }
                    if (response.headers.get('Content-Type').includes('application/octet-stream')) {
                        filename = Object.getResponseHeaderFilename(response.headers.get('Content-Disposition'));
                        return response.blob();
                    }

                    throw Error(`Error ${status}`);
                })
                .then((result) => {
                    dispatch(callActionSuccess(category));

                    if (status === 406) {
                        // result as json expected
                        dispatch(callSuccess(
                            category,
                            { validationErrors: result.validationErrors },
                        ));
                        window.scrollTo(0, 0);
                        return Promise.resolve();
                    }
                    if (filename) {
                        // result as blob expected:
                        return fileDownload(result, filename);
                    }

                    return callAction({ responseAction: result })(dispatch, getState);
                })
                .catch((error) => dispatch(callFailure(category, error)));
        }
        default:
            return Promise.reject(Error(`TargetType ${action.targetType} not implemented.`));
    }
    return Promise.resolve();
};

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
        .then((response) => response.json())
        .then((json) => {
            const { targetType } = json;
            if (targetType) {
                callAction({
                    responseAction: json,
                    watchFieldsTriggered: [],
                }, [])(dispatch, getState);
                dispatch(callSuccess(category, {}));
                return;
            }

            dispatch(callSuccess(category, Object.combine(params, json)));
        })
        .catch((error) => {
            dispatch(callFailure(category, error));
            throw error;
        });
};

export const setCurrentData = (newData) => (dispatch, getState) => {
    const { form } = getState();
    const { categories, currentCategory } = form;
    const { ui } = categories[currentCategory];

    // Change Data in redux model
    dispatch(changeData(form.currentCategory, newData));

    // Check for triggered watch fields
    if (ui.watchFields) {
        const watchFieldsTriggered = Object.keys(newData)
            .filter((key) => ui.watchFields.includes(key));

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

export const setCurrentVariables = (newVariables) => (dispatch, getState) => dispatch(
    changeVariables(getState().form.currentCategory, newVariables),
);
