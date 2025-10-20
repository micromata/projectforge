import history from '../../utilities/history';
import { fetchJsonPost, getObjectFromQuery, getServiceURL, handleHTTPErrors } from '../../utilities/rest';

export const LIST_DISMISS_ERROR = 'LIST_DISMISS_ERROR';
export const LIST_SWITCH_CATEGORY = 'LIST_SWITCH_CATEGORY';
export const LIST_FETCH_FAILURE = 'LIST_FETCH_FAILURE';
export const LIST_INITIAL_CALL_BEGIN = 'LIST_INITIAL_CALL_BEGIN';
export const LIST_FETCH_DATA_BEGIN = 'LIST_FETCH_DATA_BEGIN';
export const LIST_CALL_SUCCESS = 'LIST_CALL_SUCCESS';

const dismissError = (category) => ({
    type: LIST_DISMISS_ERROR,
    payload: { category },
});

const switchCategory = (category) => ({
    type: LIST_SWITCH_CATEGORY,
    payload: { category },
});

export const fetchFailure = (category, error) => ({
    type: LIST_FETCH_FAILURE,
    payload: {
        category,
        error,
    },
});

const initialCallBegin = (category, search) => ({
    type: LIST_INITIAL_CALL_BEGIN,
    payload: {
        category,
        search,
    },
});

const fetchDataBegin = (category, variables) => ({
    type: LIST_FETCH_DATA_BEGIN,
    payload: {
        category,
        variables,
    },
});

const callSuccess = (category, response) => ({
    type: LIST_CALL_SUCCESS,
    payload: {
        category,
        response,
    },
});

const initialCall = (category, dispatch) => {
    dispatch(initialCallBegin(category, history.location.search));

    return fetch(
        getServiceURL(
            `${category}/initialList`,
            getObjectFromQuery(history.location.search),
        ),
        {
            method: 'GET',
            credentials: 'include',
        },
    )
        .then(handleHTTPErrors)
        .then((response) => response.json())
        .then((json) => {
            const { targetType, url: redirectUrl } = json;
            if (targetType === 'REDIRECT' && redirectUrl) {
                history.push(redirectUrl);
                return;
            }
            dispatch(callSuccess(category, json));
        })
        .catch((error) => dispatch(fetchFailure(category, error.message)));
};

const fetchData = (category, dispatch, listState, variables) => {
    dispatch(fetchDataBegin(category, variables));

    return fetch(
        getServiceURL(`${category}/list`),
        {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(listState.categories[category].filter),
        },
    )
        .then(handleHTTPErrors)
        .then((response) => response.json())
        .then((data) => {
            if (data.reloadUI) {
                // Campaign or UI definitions changed - reload initial list
                // to get updated filter definitions
                return initialCall(category, dispatch);
            }
            return dispatch(callSuccess(category, { data }));
        })
        .catch((error) => dispatch(fetchFailure(category, error.message)));
};

export const dismissCurrentError = () => (dispatch, getState) => dispatch(
    dismissError(getState().list.currentCategory),
);

export const loadList = (
    category,
    ignoreLastQueriedFilters = false,
    variables = undefined,
) => (dispatch, getState) => {
    const { list } = getState();

    if (list.currentCategory !== category) {
        dispatch(switchCategory(category));
    }

    const { search } = history.location;
    const categoryData = list.categories[category];

    let fn = initialCall;

    if (categoryData) {
        // Abort when the category is currently getting fetched.
        if (categoryData.isFetching) {
            return Promise.resolve();
        }

        // Abort when filters haven't changed.
        if (
            !ignoreLastQueriedFilters
            && categoryData.lastQueriedFilter === JSON.stringify(categoryData.filter)
        ) {
            return Promise.resolve();
        }

        // Just update the entries if cached.
        if (categoryData.search === search) {
            fn = fetchData;
        }
    }

    return fn(category, dispatch, list, variables);
};

export const fetchCurrentList = (ignoreLastQueriedFilters = false) => (dispatch, getState) => {
    const category = getState().list.currentCategory;

    loadList(category, ignoreLastQueriedFilters)(dispatch, getState);
};

export const exportCurrentList = () => (dispatch, getState) => {
    const { list } = getState();
    const category = list.currentCategory;
    return fetch(
        getServiceURL(`${category}/exportAsExcel`),
        {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(list.categories[category].filter),
        },
    )
        .then(handleHTTPErrors)
        .then((response) => {
            const contentDisposition = response.headers.get('Content-Disposition'); // is null for CORS
            const filename = contentDisposition ? contentDisposition.split('filename=')[1] : `${category}Export.xls`;
            response.blob().then((blob) => {
                // https://stackoverflow.com/questions/50694881/how-to-download-file-in-react-js
                // eslint-disable-next-line
                const url = window.URL.createObjectURL(new Blob([blob]));
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', filename);
                document.body.appendChild(link);
                link.click();
                link.parentNode.removeChild(link);
            });
        })
        .catch((error) => dispatch(fetchFailure(category, error.message)));
};

export const startMultiSelection = () => (dispatch, getState) => {
    const { list } = getState();
    const category = list.currentCategory;
    return fetchJsonPost(
        `${category}/startMultiSelection`,
        list.categories[category].filter,
        (json) => {
            const { url } = json;
            history.push(url);
        },
    );
};
