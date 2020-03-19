import history from '../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';
import { loadUserStatus } from './authentication';

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

const changeData = (category, newData, watchFieldsTriggered) => ({
    type: FORM_CHANGE_DATA,
    payload: {
        category,
        newData,
        watchFieldsTriggered,
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

    if (action.targetType === 'REDIRECT') {
        history.push(`/${action.url}`, { serverData: action.variables });
        return Promise.resolve();
    }

    const { form: state } = getState();
    const category = state.currentCategory;

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

            if (status === 200 || status === 406) {
                return response.json();
            }

            throw Error(`Error ${status}`);
        })
        .then((json) => {
            dispatch(callActionSuccess(category));

            switch (status) {
                case 200:
                    switch (json.targetType) {
                        case 'REDIRECT':
                            history.push(json.url, { variables: json.variables });
                            break;
                        case 'UPDATE':
                            if (json.url) {
                                history.push(
                                    `${json.url}`,
                                    {
                                        noReload: true,
                                        newVariables: json.variables,
                                    },
                                );
                                window.scrollTo(0, 0);
                            } else {
                                dispatch(callSuccess(category, json.variables));
                            }
                            break;
                        case 'CHECK_AUTHENTICATION':
                            loadUserStatus()(dispatch);

                            if (json.url) {
                                history.push(json.url);
                            }
                            break;
                        case 'NOTHING':
                        default:
                            throw Error(`Target Type ${json.targetType} not implemented.`);
                    }
                    break;
                case 406:
                    dispatch(callSuccess(category, { validationErrors: json.validationErrors }));
                    window.scrollTo(0, 0);
                    break;
                default:
                    throw Error(`Error ${status}`);
            }
        })
        .catch(error => dispatch(callFailure(category, error)));
};

export const setCurrentData = newData => (dispatch, getState) => {
    const { form } = getState();
    const { categories, currentCategory } = form;
    const { ui } = categories[currentCategory];

    // Change Data in redux model
    dispatch(changeData(form.currentCategory, newData));

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
