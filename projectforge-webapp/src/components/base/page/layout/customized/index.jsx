import PropTypes from 'prop-types';
import React from 'react';
import CustomizedLendOutComponent from './components/LendOut';

function CustomizedLayout({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'lendOutComponent':
            Tag = CustomizedLendOutComponent;
            break;
        default:
    }

    if (!Tag) {
        return <span>{`Customzied field '${id}' not found.`}</span>;
    }

    return <Tag id={id} {...props} />;
}

CustomizedLayout.propTypes = {
    id: PropTypes.string.isRequired,
};

export default CustomizedLayout;
