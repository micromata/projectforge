import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import ReactSelect from '../ReactSelect';

function AutoCompletion({ url, ...props }) {
    const loadOptions = (search, callback) => fetch(
        getServiceURL(`${url}/${search}`),
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
        .then(json => callback(json.map(completion => ({
            value: completion,
            label: completion,
        }))));

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
};

AutoCompletion.defaultProps = {};

export default AutoCompletion;
