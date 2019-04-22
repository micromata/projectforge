import React from 'react';
import { components } from 'react-select';

const ClickableMultiValueLabel = onClick => props => (
    <div onClick={onClick} role="button" tabIndex={-1} onKeyPress={undefined}>
        <components.MultiValueLabel {...props} />
    </div>
);

export default ClickableMultiValueLabel;
