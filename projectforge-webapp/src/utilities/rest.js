const testServer = 'http://localhost:8080/rs';

// Cannot achieve coverage of 100% because of testing environment.
export const baseURL = (process.env.NODE_ENV === 'development' ? testServer : '/rs');

export const createQueryParams = params => Object.keys(params)
    .map(key => `${key}=${encodeURI(params[key])}`)
    .join('&');

export const getServiceURL = (serviceURL, params) => {
    if (params && Object.keys(params).length) {
        return `${baseURL}/${serviceURL}?${createQueryParams(params)}`;
    }

    return `${baseURL}/${serviceURL}`;
};

export const handleHTTPErrors = (response) => {
    if (!response.ok) {
        throw Error(response.status);
    }

    return response;
};

export const getObjectFromQuery = query => query
    // get each param in 'key=value' format
    .match(/[^&?]+/gm)
    // split each param to ['key', 'value']
    .map(param => param.split(/=/))
    // build the final object
    .reduce((accumulator, current) => ({
        ...accumulator,
        [current[0]]: current[1],
    }), {});
