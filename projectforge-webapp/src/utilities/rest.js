const testServer = 'http://localhost:8080';

// Save Data when saveData mode is enabled.
// https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation/saveData
export const debouncedWaitTime = (
    navigator && navigator.connection && navigator.connection.saveData
) ? 1000 : 250;

export const baseURL = process.env.NODE_ENV === 'development' ? testServer : '';
export const baseRestURL = `${baseURL}/rs`;

export const createQueryParams = params => Object.keys(params)
    .filter(key => params[key] !== undefined)
    .map(key => `${key}=${encodeURIComponent(params[key])}`)
    .join('&');

export const getServiceURL = (serviceURL, params) => {
    const top = serviceURL.startsWith('/') ? baseURL : `${baseRestURL}/`;

    return `${top}${evalServiceURL(serviceURL, params)}`;
};

export const evalServiceURL = (serviceURL, params) => {
    if (params && Object.keys(params).length) {
        return `${serviceURL}${serviceURL.includes('?') ? '&' : '?'}${createQueryParams(params)}`;
    }

    return `${serviceURL}`;
};

export const handleHTTPErrors = (response) => {
    if (!response.ok) {
        throw Error(`Fetch failed: Error ${response.status}`);
    }

    return response;
};

export const fetchJsonGet = (url, params, callback) => fetch(
    getServiceURL(url, params), {
        method: 'GET',
        credentials: 'include',
        headers: {
            Accept: 'application/json',
        },
    },
)
    .then(handleHTTPErrors)
    .then(response => response.json())
    .then(json => callback(json))
    .catch(error => alert(`Internal error: ${error}`));

export const fetchJsonPost = (url, value, callback) => fetch(
    getServiceURL(url), {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(value),
    },
)
    .then(handleHTTPErrors)
    .then(response => response.json())
    .then(json => callback(json))
    .catch(error => alert(`Internal error: ${error}`));

export const fetchGet = (url, params, callback) => fetch(
    getServiceURL(url, params), {
        method: 'GET',
        credentials: 'include',
    },
)
    .then(handleHTTPErrors)
    .then(() => callback())
    .catch(error => alert(`Internal error: ${error}`));

export const getObjectFromQuery = query => (
    query
        // get each param in 'key=value' format
        .match(/[^&?]+/gm)
    // if no matches found, work with empty array
    || []
)
    // split each param to ['key', 'value']
    .map(param => param.split(/=/))
    // build the final object
    .reduce((accumulator, current) => ({
        ...accumulator,
        [current[0]]: current[1] || true,
    }), {});
