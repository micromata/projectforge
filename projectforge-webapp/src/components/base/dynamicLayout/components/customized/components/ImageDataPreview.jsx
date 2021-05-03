import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL } from '../../../../../../utilities/rest';

function CustomizedImageDataPreview({ data }) {
    const { address, previewImageUrl } = data;

    return React.useMemo(() => {
        if (!previewImageUrl) {
            return <></>;
        }

        return (
            <img
                src={getServiceURL(previewImageUrl)}
                alt={`${address.firstName} ${address.name} (${address.organization})`}
            />
        );
    }, [previewImageUrl, address]);
}

CustomizedImageDataPreview.propTypes = {
    data: PropTypes.shape({
        previewImageUrl: PropTypes.string,
        address: PropTypes.shape({
            firstName: PropTypes.string,
            name: PropTypes.string,
            organization: PropTypes.string,
        }),
    }).isRequired,
};

export default CustomizedImageDataPreview;
