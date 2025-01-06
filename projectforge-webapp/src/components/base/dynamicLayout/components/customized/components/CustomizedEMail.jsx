import React from 'react';
import PropTypes from 'prop-types';
import EMail from '../../../../../design/EMail';

function CustomizedEMail({ values }) {
    const { data } = values;

    return React.useMemo(
        () => (
            <EMail
                email={data.email}
            />
        ),
        [data],
    );
}

CustomizedEMail.propTypes = {
    values: PropTypes.shape({}).isRequired,
};

export default CustomizedEMail;
