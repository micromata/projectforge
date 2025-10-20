import React from 'react';
import { useNavigate, useLocation } from 'react-router';
import PropTypes from 'prop-types';
import style from './Customized.module.scss';

function OpenModalLinkCell({
    value, data, urlPattern, multiline,
}) {
    const navigate = useNavigate();
    const location = useLocation();

    const handleClick = (e) => {
        e.stopPropagation();
        if (urlPattern && data) {
            // Replace placeholders in URL pattern with actual data values
            // e.g., "/react/address/edit/{addressId}" -> "/react/address/edit/123"
            let url = urlPattern;
            const matches = urlPattern.match(/\{(\w+)\}/g);
            if (matches) {
                matches.forEach((match) => {
                    const key = match.slice(1, -1); // Remove { }
                    const dataValue = data[key];
                    if (dataValue !== undefined) {
                        url = url.replace(match, dataValue);
                    }
                });
            }
            // Add modal=true query parameter so backend knows it was opened in modal context
            url += url.includes('?') ? '&modal=true' : '?modal=true';
            navigate(url, { state: { background: location } });
        }
    };

    if (!value) {
        return null;
    }

    return (
        <span
            className={`${style.clickableLink} ${multiline ? style.multiline : ''}`}
            onClick={handleClick}
            role="button"
            tabIndex={0}
            onKeyPress={(e) => {
                if (e.key === 'Enter') handleClick(e);
            }}
        >
            {multiline
                ? value.split('\n').map((line, index) => (
                    // eslint-disable-next-line react/no-array-index-key
                    <React.Fragment key={`line-${index}`}>
                        {line}
                        {index < value.split('\n').length - 1 && <br />}
                    </React.Fragment>
                ))
                : value}
        </span>
    );
}

OpenModalLinkCell.propTypes = {
    value: PropTypes.string,
    data: PropTypes.shape({}).isRequired,
    urlPattern: PropTypes.string.isRequired,
    multiline: PropTypes.bool,
};

OpenModalLinkCell.defaultProps = {
    value: null,
    multiline: false,
};

export default OpenModalLinkCell;
