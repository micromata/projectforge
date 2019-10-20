import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import ReactSelect from '../ReactSelect';

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
        .then(json => callback(json.map((completion) => {
            switch (type) {
                case 'USER':
                    return ({
                        label: completion.fullname,
                        value: completion.id,
                    });
                default:
                    return ({
                        value: completion,
                        label: completion,
                    });
            }
        })));

    return (
        <ReactSelect
            translations={{}}
            loadOptions={loadOptions}
            {...props}
        />
    );
}

AutoCompletion.propTypes = {
    url: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['USER', undefined]),
};

AutoCompletion.defaultProps = {
    type: undefined,
};

export default AutoCompletion;
