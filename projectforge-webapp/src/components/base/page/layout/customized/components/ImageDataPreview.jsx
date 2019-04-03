import React from 'react';
import { dataPropType } from '../../../../../../utilities/propTypes';
import { getServiceURL } from '../../../../../../utilities/rest';

function CustomizedImageDataPreview({ data: { previewImageUrl, address } }) {
    if (!previewImageUrl) {
        return <React.Fragment />;
    }

    return (
        <img
            src={getServiceURL(previewImageUrl)}
            alt={`${address.firstName} ${address.name} (${address.organization})`}
        />
    );
}

CustomizedImageDataPreview.propTypes = {
    data: dataPropType.isRequired,
};

export default CustomizedImageDataPreview;
