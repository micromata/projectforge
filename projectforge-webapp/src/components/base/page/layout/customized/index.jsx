import PropTypes from 'prop-types';
import React from 'react';
import CustomizedAddressImage from './components/AddressImage';
import CustomizedImageDataPreview from './components/ImageDataPreview';
import CustomizedLendOutComponent from './components/LendOut';

function CustomizedLayout({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'lendOutComponent':
            Tag = CustomizedLendOutComponent;
            break;
        case 'addressImage':
            Tag = CustomizedAddressImage;
            break;
        case 'imageDataPreview':
            Tag = CustomizedImageDataPreview;
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
