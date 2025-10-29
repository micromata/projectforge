import React from 'react';
import PropTypes from 'prop-types';
import style from './Customized.module.scss';
import { getServiceURL } from '../../../../../../utilities/rest';

function CustomizedImage({ values }) {
    return React.useMemo(
        () => (
            <img
                className={style.addressImage}
                src={getServiceURL(values.src)}
                alt={values.alt}
                style={{
                    maxWidth: '400px',
                    maxHeight: '200px',
                    width: 'auto',
                    height: 'auto',
                    objectFit: 'contain',
                }}
            />
        ),
        [
            values.imageData,
            values.alt,
            values.src,
        ],
    );
}

CustomizedImage.propTypes = {
    values: PropTypes.shape({
        src: PropTypes.string.isRequired,
        alt: PropTypes.string,
    }).isRequired,
};

export default CustomizedImage;
