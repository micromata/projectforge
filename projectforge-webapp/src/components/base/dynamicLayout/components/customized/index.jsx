import PropTypes from 'prop-types';
import React from 'react';
import BookLendOut from './components/BookLendOut';
import CustomizedAddressImage from './components/CustomizedAddressImage';
import DayRange from './components/DayRange';
import CustomizedImageDataPreview from './components/ImageDataPreview';

function DynamicCustomized({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'address.edit.image':
            Tag = CustomizedAddressImage;
            break;
        case 'address.imagePreview':
            Tag = CustomizedImageDataPreview;
            break;
        case 'book.lendOutComponent':
            Tag = BookLendOut;
            break;
        case 'dayRange':
            Tag = DayRange;
            break;
            // TODO: FIX taskAndKost2
        case 'timesheet.edit.taskAndKost2':
        default:
            return <span>{`Customized field '${id}' not found!`}</span>;
    }

    return (
        <Tag id={id} {...props} />
    );
}

DynamicCustomized.propTypes = {
    id: PropTypes.string.isRequired,
};

DynamicCustomized.defaultProps = {};

export default DynamicCustomized;
