import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import ReactSelect from '../ReactSelect';

const resolveJSON = (callback, type = undefined) => json => callback(json.map((completion) => {
    switch (type) {
        case 'USER':
            return ({
                label: completion.fullname,
                value: completion.id,
            });
        case 'RAW':
            return completion;
        default:
            return ({
                value: completion,
                label: completion,
            });
    }
}));

function AutoCompletion({ url, type, ...props }) {
    const loadOptions = (search, callback) => fetch(
        getServiceURL(`${url}${search}`),
        {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(resolveJSON(callback, type));

    return (
        <ReactSelect
            translations={{}}
            loadOptions={loadOptions}
            {...props}
            autoCompletion={{
                url,
                type,
            }}
        />
    );
}

AutoCompletion.propTypes = {
    url: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['USER', 'RAW', undefined]),
};

AutoCompletion.defaultProps = {
    type: undefined,
};

export { resolveJSON };
export default AutoCompletion;
