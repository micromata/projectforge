import PropTypes from 'prop-types';
import React, { Component } from 'react';
import {
    Button,
    Container,
    Popover,
    PopoverBody,
    PopoverHeader,
    UncontrolledTooltip
} from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faStar } from '@fortawesome/free-regular-svg-icons';
import { faEdit, faSync, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import style from '../../components/design/input/Input.module.scss';

/**
 * Favorite filters and settings of the user.
 */
class FavoritesPanel extends Component {
    constructor(props) {
        super(props);
        this.state = {
            popoverOpen: false,
        };

        this.togglePopover = this.togglePopover.bind(this);
    }

    togglePopover() {
        this.setState(prevState => ({
            popoverOpen: !prevState.popoverOpen,
        }));
    }

    render() {
        const { popoverOpen } = this.state;
        const {
            currentFavoriteId,
            favorites,
            onFavoriteDelete,
            onFavoriteUpdate,
            translations,
        } = this.props;
        return (
            <React.Fragment>
                <Button
                    id="favoritesPopover"
                    color="link"
                    className="selectPanelIconLinks"
                    onClick={this.togglePopover}
                >
                    <FontAwesomeIcon
                        icon={faStar}
                        className={style.icon}
                        size="lg"
                    />
                </Button>
                <Popover
                    placement="left-start"
                    isOpen={popoverOpen}
                    target="favoritesPopover"
                    toggle={this.togglePopover}
                    trigger="legacy"
                >
                    <PopoverHeader toggle={this.settingsPopoverOpen}>
                        {translations.favorites}
                    </PopoverHeader>
                    <PopoverBody>
                        <Container>
                            <ul>
                                {favorites.map(favorite => (
                                    <li key={favorite.id}>
                                        {favorite.name}
                                        <Button
                                            id={`ren-${favorite.id}`}
                                            color="link"
                                            disabled
                                        >
                                            <FontAwesomeIcon
                                                icon={faEdit}
                                                className={style.icon}
                                                color="grey"
                                            />
                                        </Button>
                                        <UncontrolledTooltip
                                            placement="right"
                                            target={`ren-${favorite.id}`}
                                        >
                                            {translations.rename}
                                        </UncontrolledTooltip>
                                        <Button
                                            id={`del-${favorite.id}`}
                                            color="link"
                                            onClick={() => onFavoriteDelete(favorite.id)}
                                        >
                                            <FontAwesomeIcon
                                                icon={faTrashAlt}
                                                className={style.icon}
                                                color="tomato"
                                            />
                                        </Button>
                                        <UncontrolledTooltip
                                            placement="right"
                                            target={`del-${favorite.id}`}
                                        >
                                            {translations.delete}
                                        </UncontrolledTooltip>
                                        {' '}
                                        {favorite.id === currentFavoriteId ? (
                                            <React.Fragment>
                                                <Button
                                                    id="syncFavorite"
                                                    color="link"
                                                    onClick={() => onFavoriteUpdate(favorite.id)}
                                                >
                                                    <FontAwesomeIcon
                                                        icon={faSync}
                                                        className={style.icon}
                                                        color="grey"
                                                    />
                                                </Button>
                                                <UncontrolledTooltip
                                                    placement="right"
                                                    target="syncFavorite"
                                                >
                                                    {translations.save}
                                                </UncontrolledTooltip>
                                            </React.Fragment>
                                        ) : ''}
                                    </li>
                                ))}
                            </ul>
                        </Container>
                    </PopoverBody>
                </Popover>
            </React.Fragment>
        );
    }
}

FavoritesPanel.propTypes = {
    favorites: PropTypes.shape({
        id: PropTypes.number,
        name: PropTypes.string,
    }),
    currentFavoriteId: PropTypes.number, // The current used favorite (can be saved with modified settings).
    onFavoriteDelete: PropTypes.func.isRequired,
    onFavoriteRename: PropTypes.func.isRequired,
    onFavoriteSelect: PropTypes.func.isRequired,
    onFavoriteUpdate: PropTypes.func.isRequired,
    translations: PropTypes.shape({}).isRequired,
};

FavoritesPanel.defaultProps = {
    currentFavoriteId: 2,
    favorites: [{
        id: 1,
        name: 'Filter 1',
    }, {
        id: 2,
        name: 'Filter 2',
    }, {
        id: 3,
        name: 'Filter 3',
    }, {
        id: 4,
        name: 'Filter 4',
    }, {
        id: 5,
        name: 'Filter 5',
    }, {
        id: 6,
        name: 'Filter 6',
    }, {
        id: 7,
        name: 'Filter 7',
    }],
};

export default (FavoritesPanel);
