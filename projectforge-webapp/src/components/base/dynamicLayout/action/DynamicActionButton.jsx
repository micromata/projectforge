import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { Button } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicActionButton(props) {
    const {
        default: isDefault,
        style,
        title,
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
            color={style}
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
    style: colorPropType,
};

DynamicActionButton.defaultProps = {
    default: false,
    style: 'secondary',
};

export default DynamicActionButton;
