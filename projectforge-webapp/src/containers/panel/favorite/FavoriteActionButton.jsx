import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import style from '../../../components/design/input/Input.module.scss';

function FavoriteActionButton(
    {
        className,
        icon,
        id,
        onClick,
        size,
        tooltip,
    },
) {
    const handleClick = (event) => {
        event.stopPropagation();

        onClick(event);
    };

    return (
        <>
            <FontAwesomeIcon
                id={id}
                icon={icon}
                className={classNames(style.icon, className)}
                onClick={handleClick}
                size={size}
            />
            {tooltip && id ? (
                <UncontrolledTooltip placement="right" target={id} fade timeout={150}>
                    {tooltip}
                </UncontrolledTooltip>
            ) : undefined}
        </>
    );
}

FavoriteActionButton.propTypes = {
    icon: PropTypes.shape({}).isRequired,
    onClick: PropTypes.func.isRequired,
    className: PropTypes.string,
    id: PropTypes.string,
    size: PropTypes.string,
    tooltip: PropTypes.string,
};

export default FavoriteActionButton;
