const testServer = 'http://localhost:8080/rest';
const baseURL = (process.env.NODE_ENV === 'development' ? testServer : '/rest');

const createQueryParams = params => Object.keys(params)
    .map(key => `${key}=${encodeURI(params[key])}`)
    .join('&');

/* eslint-disable-next-line import/prefer-default-export */
export const getServiceURL = (serviceURL, params) => {
    if (params) {
        return `${baseURL}/${serviceURL}?${createQueryParams(params)}`;
    }

    return `${baseURL}/${serviceURL}`;
};
