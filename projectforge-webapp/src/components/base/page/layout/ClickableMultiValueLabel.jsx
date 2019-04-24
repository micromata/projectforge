import React from 'react';
import { components } from 'react-select';

const handleClick = (onClick, props) => event => onClick(event, props);

const ClickableMultiValueLabel = onClick => props => (
    <div
        onClick={handleClick(onClick, props)}
        role="button"
        tabIndex={-1}
        onKeyPress={undefined}
    >
        <components.MultiValueLabel {...props} />
    </div>
);

export default ClickableMultiValueLabel;
