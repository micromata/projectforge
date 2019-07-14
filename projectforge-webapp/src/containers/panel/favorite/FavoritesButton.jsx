import { faStar as faStarRegular } from '@fortawesome/free-regular-svg-icons';
import { faStar as faStarSolid } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'reactstrap';
import style from '../../../components/design/input/Input.module.scss';

function FavoritesButton({ id, isOpen, toggle }) {
    const [hover, setHover] = React.useState(false);

    const handleOpenButtonHover = ({ type }) => setHover(
        type === 'mouseover' || type === 'focus',
    );

    return (
        <Button
            id={id}
            color="link"
            className="selectPanelIconLinks"
            onClick={toggle}
            onFocus={handleOpenButtonHover}
            onBlur={handleOpenButtonHover}
            onMouseOver={handleOpenButtonHover}
            onMouseLeave={handleOpenButtonHover}
        >
            <FontAwesomeIcon
                icon={hover || isOpen ? faStarSolid : faStarRegular}
                className={style.icon}
                size="lg"
            />
        </Button>
    );
}

FavoritesButton.propTypes = {
    id: PropTypes.string.isRequired,
    toggle: PropTypes.func.isRequired,
    isOpen: PropTypes.bool,
};

FavoritesButton.defaultProps = {
    isOpen: false,
};

export default FavoritesButton;
