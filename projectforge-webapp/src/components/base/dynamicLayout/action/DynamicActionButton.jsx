import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { Button } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicActionButton(
    {
        default: isDefault,
        id,
        style,
        title,
    },
) {
    const { callAction } = React.useContext(DynamicLayoutContext);

    const handleClick = (event) => {
        event.preventDefault();
        event.stopPropagation();

        callAction(id);
    };

    let type = 'button';

    if (isDefault) {
        type = 'submit';
    }

    return (
        <Button
            color={style}
            onClick={handleClick}
            type={type}
        >
            {title}
        </Button>
    );
}

DynamicActionButton.propTypes = {
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    default: PropTypes.bool,
    style: colorPropType,
};

DynamicActionButton.defaultProps = {
    default: false,
    style: 'secondary',
};

export default DynamicActionButton;
