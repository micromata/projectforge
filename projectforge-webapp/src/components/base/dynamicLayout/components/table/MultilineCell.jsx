import React from 'react';
import PropTypes from 'prop-types';

/**
 * Custom AG Grid cell renderer for multiline text.
 * Converts newline characters (\n) into actual line breaks.
 */
function MultilineCell({ value }) {
    if (!value) {
        return null;
    }

    // Split by newline characters and render each line
    const lines = value.split('\n');

    return (
        <div style={{ whiteSpace: 'pre-line' }}>
            {lines.map((line, index) => (
                // eslint-disable-next-line react/no-array-index-key
                <React.Fragment key={index}>
                    {line}
                    {index < lines.length - 1 && <br />}
                </React.Fragment>
            ))}
        </div>
    );
}

MultilineCell.propTypes = {
    value: PropTypes.string,
};

export default MultilineCell;
