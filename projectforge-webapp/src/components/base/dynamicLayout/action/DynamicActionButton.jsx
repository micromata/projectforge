import PropTypes from 'prop-types';
import React from 'react';
import { Button } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicActionButton(props) {
    const {
        default: isDefault,
        title,
        ...stylingProps
    } = props;

    const { callAction } = React.useContext(DynamicLayoutContext);

    const handleClick = (event) => {
        event.preventDefault();
        event.stopPropagation();

        callAction(props);
    };

    let type = 'button';

    if (isDefault) {
        type = 'submit';
    }

    return (
        <Button
            onClick={handleClick}
            type={type}
            {...stylingProps}
        >
            {title}
        </Button>
    );
}

DynamicActionButton.propTypes = {
    title: PropTypes.string.isRequired,
    default: PropTypes.bool,
};

DynamicActionButton.defaultProps = {
    default: false,
};

export default DynamicActionButton;
