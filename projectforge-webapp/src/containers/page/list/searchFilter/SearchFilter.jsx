import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Navbar } from 'reactstrap';
import { fetchListFavorites } from '../../../../actions';
import { changeSearchString } from '../../../../actions/list/filter';
import Navigation from '../../../../components/base/navigation';
import { Col, Row } from '../../../../components/design';
import AdvancedPopper from '../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../components/design/popper/AdvancedPopperAction';
import { debouncedWaitTime, getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import FavoritesPanel from '../../../panel/favorite/FavoritesPanel';
import styles from '../ListPage.module.scss';
import MagicFilters from './magicFilter/MagicFilters';
import QuickSelectionEntry from './QuickSelectionEntry';
import SearchField from './SearchField';

const loadQuickSelectionsBounced = (
    {
        url,
        searchString = '',
        setQuickSelections,
    },
) => {
    fetch(
        getServiceURL(url.replace(':searchString', encodeURIComponent(searchString))),
        {
            method: 'GET',
            credentials: 'include',
            headers: { Accept: 'application/json' },
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setQuickSelections);
};

function SearchFilter(props) {
    const {
        category,
        onFavoriteCreate,
        onFavoriteDelete,
        onFavoriteRename,
        onFavoriteSelect,
        onFavoriteUpdate,
        onSearchStringChange,
        onSearchStringDelete,
    } = props;

    const {
        filter,
        filterFavorites,
        quickSelectUrl,
        ui,
    } = category;

    const [quickSelections, setQuickSelections] = React.useState([]);
    const [searchActive, setSearchActive] = React.useState(false);
    const [loadQuickSelections] = React.useState(
        () => AwesomeDebouncePromise(loadQuickSelectionsBounced, debouncedWaitTime),
    );

    // Initial QuickSelections call. Recall when url changed.
    React.useEffect(() => {
        if (quickSelectUrl) {
            loadQuickSelections({
                url: quickSelectUrl,
                searchString: filter.searchString,
                setQuickSelections,
            });
        }
    }, [quickSelectUrl, filter.searchString]);

    return (
        <React.Fragment>
            <Row>
                <Col sm={4}>
                    <AdvancedPopper
                        additionalClassName={styles.completions}
                        setIsOpen={setSearchActive}
                        isOpen={searchActive}
                        basic={(
                            <SearchField
                                id="searchString"
                                onChange={onSearchStringChange}
                                value={filter.searchString}
                            />
                        )}
                        className={styles.searchContainer}
                        actions={(
                            <AdvancedPopperAction
                                type="delete"
                                disabled={!filter.searchString}
                                onClick={onSearchStringDelete}
                            >
                                {ui.translations.delete || ''}
                            </AdvancedPopperAction>
                        )}
                    >
                        {quickSelectUrl && (
                            <React.Fragment>
                                <ul className={styles.entries}>
                                    {/* TODO ADD KEYBOARD LISTENER FOR SELECTING */}
                                    {quickSelections.map(({ id, displayName }) => (
                                        <QuickSelectionEntry
                                            key={`quick-selection-${id}`}
                                            id={id}
                                            displayName={displayName}
                                        />
                                    ))}
                                </ul>
                                {quickSelections.length === 0 && (
                                    <p className={styles.errorMessage}>
                                        ???No quick selections found.???
                                    </p>
                                )}
                            </React.Fragment>
                        )}
                    </AdvancedPopper>
                </Col>
                <Col sm={1} className="d-flex align-items-center">
                    <FavoritesPanel
                        onFavoriteCreate={onFavoriteCreate}
                        onFavoriteDelete={onFavoriteDelete}
                        onFavoriteRename={onFavoriteRename}
                        onFavoriteSelect={onFavoriteSelect}
                        onFavoriteUpdate={onFavoriteUpdate}
                        favorites={filterFavorites}
                        currentFavoriteId={filter.id}
                        isModified
                        closeOnSelect={false}
                        translations={ui.translations}
                        htmlId="searchFilterFavoritesPopover"
                    />
                </Col>
                {/* Render the menu if it's loaded. */}
                {ui && ui.pageMenu && (
                    <Col>
                        <Navbar>
                            <Navigation
                                entries={ui.pageMenu}
                                // Let the menu float to the right.
                                className="ml-auto"
                            />
                        </Navbar>
                    </Col>
                )}
            </Row>
            <hr />
            <MagicFilters />
            <hr />
        </React.Fragment>
    );
}

SearchFilter.propTypes = {
    category: PropTypes.shape({
        filter: PropTypes.shape({}),
        filterFavorites: PropTypes.arrayOf(PropTypes.shape({})),
    }).isRequired,
    onFavoriteCreate: PropTypes.func.isRequired,
    onFavoriteDelete: PropTypes.func.isRequired,
    onFavoriteRename: PropTypes.func.isRequired,
    onFavoriteSelect: PropTypes.func.isRequired,
    onFavoriteUpdate: PropTypes.func.isRequired,
    onSearchStringChange: PropTypes.func.isRequired,
    onSearchStringDelete: PropTypes.func.isRequired,
};

SearchFilter.defaultProps = {};

const mapStateToProps = ({ list }) => {
    const category = list.categories[list.currentCategory];

    return {
        category,
        filter: category.filter,
    };
};

const actions = (dispatch, { filter }) => ({
    onFavoriteCreate: name => dispatch(fetchListFavorites('create', {
        body: {
            ...filter,
            name,
        },
    })),
    onFavoriteDelete: id => dispatch(fetchListFavorites('delete', { params: { id } })),
    onFavoriteRename: (id, newName) => fetchListFavorites('rename', {
        params: {
            id,
            newName,
        },
    }),
    onFavoriteSelect: id => dispatch(fetchListFavorites('select', { params: { id } })),
    onFavoriteUpdate: () => dispatch(fetchListFavorites('update', { body: filter })),
    onSearchStringChange: ({ target }) => dispatch(changeSearchString(target.value)),
    onSearchStringDelete: () => dispatch(changeSearchString('')),
});

export default connect(mapStateToProps, actions)(SearchFilter);
