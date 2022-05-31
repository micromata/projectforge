import PropTypes from 'prop-types';
import React from 'react';

function EMail(
    {
        email,
    },
) {
    return (
        <a href={`mailto:${email}`}>{email}</a>
    );
}

EMail.propTypes = {
    email: PropTypes.string.isRequired,
};

export default EMail;
