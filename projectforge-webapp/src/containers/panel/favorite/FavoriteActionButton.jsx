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
        tooltip,
    },
) {
    const handleClick = (event) => {
        event.stopPropagation();

        onClick(event);
    };

    return (
        <React.Fragment>
            <FontAwesomeIcon
                id={id}
                icon={icon}
                className={classNames(style.icon, className)}
                onClick={handleClick}
            />
            {tooltip ? (
                <UncontrolledTooltip placement="right" target={id}>
                    {tooltip}
                </UncontrolledTooltip>
            ) : undefined}
        </React.Fragment>
    );
}

FavoriteActionButton.propTypes = {
    icon: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    onClick: PropTypes.func.isRequired,
    className: PropTypes.string,
    tooltip: PropTypes.string,
};

FavoriteActionButton.defaultProps = {
    className: undefined,
    tooltip: undefined,
};

export default FavoriteActionButton;
