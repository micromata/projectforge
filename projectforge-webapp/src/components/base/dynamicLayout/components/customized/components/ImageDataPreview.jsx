import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL } from '../../../../../../utilities/rest';

function CustomizedImageDataPreview({ data }) {
    const { address, read, previewImageUrl } = data;

    // Support both address (for address list) and read (for import table)
    const addr = address || read;

    return React.useMemo(() => {
        if (!previewImageUrl) {
            return null;
        }

        return (
            <img
                src={getServiceURL(previewImageUrl)}
                alt={`${addr?.firstName || ''} ${addr?.name || ''} (${addr?.organization || ''})`}
                style={{
                    maxWidth: '50px',
                    maxHeight: '50px',
                    width: 'auto',
                    height: 'auto',
                    objectFit: 'contain',
                }}
            />
        );
    }, [previewImageUrl, addr]);
}

CustomizedImageDataPreview.propTypes = {
    data: PropTypes.shape({
        previewImageUrl: PropTypes.string,
        address: PropTypes.shape({
            firstName: PropTypes.string,
            name: PropTypes.string,
            organization: PropTypes.string,
        }),
        read: PropTypes.shape({
            firstName: PropTypes.string,
            name: PropTypes.string,
            organization: PropTypes.string,
        }),
    }).isRequired,
};

export default CustomizedImageDataPreview;
