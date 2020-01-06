import React from 'react';

function MagicInputNotImplemented() {
    return (
        <span>Input is not implemented.</span>
    );
}

MagicInputNotImplemented.propTypes = {};

MagicInputNotImplemented.defaultProps = {};

MagicInputNotImplemented.getLabel = label => `${label} (Not implemented)`;

export default MagicInputNotImplemented;
