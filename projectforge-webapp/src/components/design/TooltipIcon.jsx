import { faQuestion } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';

function TooltipIcon() {
    return (
        <FontAwesomeIcon
            icon={faQuestion}
            size="sm"
            style={{ color: '#FF8633', marginLeft: '.5em' }}
        />
    );
}

TooltipIcon.propTypes = {};

export default TooltipIcon;
