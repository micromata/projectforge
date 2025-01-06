import { faStar as faStarRegular } from '@fortawesome/free-regular-svg-icons';
import { faStar as faStarSolid } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, UncontrolledTooltip } from 'reactstrap';
import style from '../../../components/design/input/Input.module.scss';

function FavoritesButton(
    {
        id,
        isOpen = false,
        toggle,
        favoriteButtonText,
        favoriteButtonTooltip,
        translations = {},
    },
) {
    const [hover, setHover] = React.useState(false);

    const handleOpenButtonHover = ({ type }) => setHover(
        type === 'mouseover' || type === 'focus',
    );

    const button = favoriteButtonText || (
        <FontAwesomeIcon
            icon={hover || isOpen ? faStarSolid : faStarRegular}
            className={style.icon}
            size="lg"
        />
    );

    return (
        <>
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
                {button}
            </Button>
            <UncontrolledTooltip placement="bottom" target={id}>
                {favoriteButtonTooltip || translations.favorites}
            </UncontrolledTooltip>
        </>
    );
}

FavoritesButton.propTypes = {
    id: PropTypes.string.isRequired,
    toggle: PropTypes.func.isRequired,
    favoriteButtonText: PropTypes.string,
    favoriteButtonTooltip: PropTypes.string,
    isOpen: PropTypes.bool,
    translations: PropTypes.shape({
        favorites: PropTypes.string.isRequired,
    }),
};

export default FavoritesButton;
