import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Navbar } from 'reactstrap';
import {
    dismissCurrentError,
    fetchCurrentList,
    fetchListFavorites,
    openEditPage,
} from '../../../../actions';
import { changeSearchString } from '../../../../actions/list/filter';
import Navigation from '../../../../components/base/navigation';
import { Alert, Col, Spinner } from '../../../../components/design';
import AutoCompletion from '../../../../components/design/input/autoCompletion/';
import AdvancedPopperAction from '../../../../components/design/popper/AdvancedPopperAction';
import FavoritesPanel from '../../../panel/favorite/FavoritesPanel';
import styles from '../ListPage.module.scss';
import MagicFilters from './magicFilter/MagicFilters';
import SearchField from './SearchField';

function SearchFilter(props) {
    const {
        category,
        onErrorDismiss,
        onFavoriteCreate,
        onFavoriteDelete,
        onFavoriteRename,
        onFavoriteSelect,
        onFavoriteUpdate,
        onSearchStringBlur,
        onSearchStringChange,
        onSearchStringDelete,
        onSelectQuickSelection,
    } = props;

    const {
        error,
        filter,
        filterFavorites,
        isFetching,
        quickSelectUrl,
        ui,
    } = category;

    return (
        <React.Fragment>
            <div className={styles.searchRow}>
                <AutoCompletion
                    actions={(
                        <AdvancedPopperAction
                            type="delete"
                            disabled={!filter.searchString}
                            onClick={onSearchStringDelete}
                        >
                            {ui.translations.delete || ''}
                        </AdvancedPopperAction>
                    )}
                    className={styles.searchContainer}
                    input={({ ref, ...searchProps }) => (
                        <SearchField
                            forwardRef={ref}
                            id="searchString"
                            onBlur={onSearchStringBlur}
                            onChange={onSearchStringChange}
                            {...searchProps}
                        />
                    )}
                    onSelect={onSelectQuickSelection}
                    search={filter.searchString}
                    url={quickSelectUrl}
                />
                <div className={styles.container}>
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
                    {isFetching && <Spinner className={styles.loadingSpinner} />}
                </div>
                <div className={styles.container}>
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
                </div>
            </div>
            <MagicFilters />
            <hr />
            <Alert
                color="danger"
                className={styles.alert}
                toggle={onErrorDismiss}
                isOpen={error !== undefined}
            >
                <h4>Oh Snap!</h4>
                <p>Error while contacting the server. Please contact an administrator.</p>
            </Alert>
        </React.Fragment>
    );
}

SearchFilter.propTypes = {
    category: PropTypes.shape({
        filter: PropTypes.shape({}),
        filterFavorites: PropTypes.arrayOf(PropTypes.shape({})),
    }).isRequired,
    onErrorDismiss: PropTypes.func.isRequired,
    onFavoriteCreate: PropTypes.func.isRequired,
    onFavoriteDelete: PropTypes.func.isRequired,
    onFavoriteRename: PropTypes.func.isRequired,
    onFavoriteSelect: PropTypes.func.isRequired,
    onFavoriteUpdate: PropTypes.func.isRequired,
    onSearchStringBlur: PropTypes.func.isRequired,
    onSearchStringChange: PropTypes.func.isRequired,
    onSearchStringDelete: PropTypes.func.isRequired,
    onSelectQuickSelection: PropTypes.func.isRequired,
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
    onErrorDismiss: () => dispatch(dismissCurrentError()),
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
    onSearchStringBlur: () => dispatch(fetchCurrentList()),
    onSearchStringChange: ({ target }) => dispatch(changeSearchString(target.value)),
    onSearchStringDelete: () => dispatch(changeSearchString('')),
    onSelectQuickSelection: id => dispatch(openEditPage(id)),
});

export default connect(mapStateToProps, actions)(SearchFilter);
