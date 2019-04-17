import PropTypes from 'prop-types';
import React from 'react';
import CustomizedAddressImage from './components/AddressImage';
import CustomizedImageDataPreview from './components/ImageDataPreview';
import CustomizedBookLendOutComponent from './components/BookLendOut';

function CustomizedLayout({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'book.lendOutComponent':
            Tag = CustomizedBookLendOutComponent;
            break;
        case 'address.edit.image':
            Tag = CustomizedAddressImage;
            break;
        case 'address.imagePreview':
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
