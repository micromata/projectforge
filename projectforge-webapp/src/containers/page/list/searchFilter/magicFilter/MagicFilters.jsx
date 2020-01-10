import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { resetAllFilters } from '../../../../../actions/list/filter';
import AdvancedPopper from '../../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../../components/design/popper/AdvancedPopperAction';
import { getNamedContainer } from '../../../../../utilities/layout';
import styles from '../../ListPage.module.scss';
import SearchField from '../SearchField';
import FilterListEntry from './FilterListEntry';
import MagicFilterPill from './MagicFilterPill';

function MagicFilters(
    {
        filterEntries,
        onResetAllFilters,
        searchFilter,
        searchString,
        translations,
    },
) {
    const [allFiltersAreOpen, setAllFiltersAreOpen] = React.useState(false);
    const [search, setSearch] = React.useState('');
    const searchRef = React.useRef(null);

    const setIsOpen = (open) => {
        if (searchRef.current) {
            if (open) {
                searchRef.current.focus({ preventScroll: true });
            } else {
                searchRef.current.blur();
            }
        }

        setAllFiltersAreOpen(open);
    };

    const handleSearchChange = ({ target }) => setSearch(target.value);

    const handleAllFiltersDelete = () => {
        setAllFiltersAreOpen(false);
        onResetAllFilters();
    };

    const handleAfterSelectFilter = () => setAllFiltersAreOpen(false);

    const searchLowerCase = search.toLowerCase();
    const filteredSearchFilters = searchFilter && searchFilter.content
        .filter(({ id, label }) => (
            id.toLowerCase()
                .includes(searchLowerCase)
            || label.toLowerCase()
                .includes(searchLowerCase)
        ));

    return (
        <div className={styles.magicFilters}>
            {searchFilter && filterEntries
                .map(entry => ({
                    ...Array.findByField(searchFilter.content, 'id', entry.field),
                    ...entry,
                }))
                .filter(({ id }) => id !== undefined)
                .map(entry => (
                    <MagicFilterPill
                        key={`magic-filter-${entry.id}`}
                        {...entry}
                    />
                ))}
            <div className={styles.magicFilter}>
                <AdvancedPopper
                    setIsOpen={setIsOpen}
                    isOpen={allFiltersAreOpen}
                    basic="???Alle Filter???"
                    className={styles.allFilters}
                    contentClassName={classNames(
                        styles.pill,
                        { [styles.marked]: allFiltersAreOpen },
                    )}
                    actions={(
                        <AdvancedPopperAction
                            type="delete"
                            disabled={!(filterEntries.length || searchString)}
                            onClick={handleAllFiltersDelete}
                        >
                            {translations.reset || '???Zurücksetzen???'}
                        </AdvancedPopperAction>
                    )}
                >
                    <SearchField
                        forwardRef={searchRef}
                        dark
                        id="magicFiltersSearch"
                        onCancel={() => setIsOpen(false)}
                        onChange={handleSearchChange}
                        value={search}
                    />
                    {searchFilter && (
                        <ul className={styles.filterList}>
                            {filteredSearchFilters.map(({ id, label }) => (
                                <FilterListEntry
                                    key={`filter-${id}`}
                                    afterSelect={handleAfterSelectFilter}
                                    id={id}
                                    label={label}
                                />
                            ))}
                            {filteredSearchFilters.length === 0 && (
                                <span className={styles.errorMessage}>???No Entries found???</span>
                            )}
                        </ul>
                    )}
                </AdvancedPopper>
            </div>
        </div>
    );
}

MagicFilters.propTypes = {
    translations: PropTypes.shape({
        reset: PropTypes.string,
    }).isRequired,
    filterEntries: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    onResetAllFilters: PropTypes.func.isRequired,
    searchFilter: PropTypes.shape({}),
    searchString: PropTypes.string,
};

MagicFilters.defaultProps = {
    searchFilter: undefined,
    searchString: undefined,
};

const mapStateToProps = ({ list }) => {
    const { ui, filter } = list.categories[list.currentCategory];

    return {
        translations: ui.translations,
        searchFilter: getNamedContainer('searchFilter', ui.namedContainers),
        filterEntries: filter.entries,
        searchString: filter.searchString,
    };
};

const actions = dispatch => ({
    onResetAllFilters: () => dispatch(resetAllFilters()),
});

export default connect(mapStateToProps, actions)(MagicFilters);
