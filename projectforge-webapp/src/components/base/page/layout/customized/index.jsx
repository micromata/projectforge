import PropTypes from 'prop-types';
import React from 'react';
import CustomizedAddressImage from './components/CustomizedAddressImage';
import CustomizedImageDataPreview from './components/ImageDataPreview';
import CustomizedBookLendOutComponent from './components/BookLendOut';
import DayRange from './components/DayRange';
import TimesheetEditTaskAndKost2 from './components/TimesheetEditTaskAndKost2';

function CustomizedLayout({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'dayRange':
            Tag = DayRange;
            break;
        case 'book.lendOutComponent':
            Tag = CustomizedBookLendOutComponent;
            break;
        case 'timesheet.edit.taskAndKost2':
            Tag = TimesheetEditTaskAndKost2;
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
