import PropTypes from 'prop-types';
import React from 'react';
import { Button } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicActionButton(props) {
    const {
        default: isDefault,
        title,
        responseAction,
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
            {...stylingProps}
            onClick={handleClick}
            type={type}
        >
            {title}
        </Button>
    );
}

DynamicActionButton.propTypes = {
    title: PropTypes.string.isRequired,
    default: PropTypes.bool,
    responseAction: PropTypes.shape({}),
};

DynamicActionButton.defaultProps = {
    default: false,
    responseAction: {},
};

export default DynamicActionButton;
