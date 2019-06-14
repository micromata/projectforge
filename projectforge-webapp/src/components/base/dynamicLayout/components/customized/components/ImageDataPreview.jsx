import React from 'react';
import { getServiceURL } from '../../../../../../utilities/rest';
import { DynamicLayoutContext } from '../../../context';

function CustomizedImageDataPreview() {
    const {
        data: { previewImageUrl, address },
    } = React.useContext(DynamicLayoutContext);

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

CustomizedImageDataPreview.propTypes = {};

export default CustomizedImageDataPreview;
