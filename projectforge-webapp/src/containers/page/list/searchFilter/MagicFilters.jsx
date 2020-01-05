import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { resetAllFilters } from '../../../../actions/list/filter';
import AdvancedPopper from '../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../components/design/popper/AdvancedPopperAction';
import { getNamedContainer } from '../../../../utilities/layout';
import styles from '../ListPage.module.scss';
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

    const handleAllFiltersDelete = () => {
        setAllFiltersAreOpen(false);
        onResetAllFilters();
    };

    const handleAfterSelectFilter = () => setAllFiltersAreOpen(false);

    return (
        <div className={styles.magicFilters}>
            {searchFilter && filterEntries
                .map(({ field, value }) => ({
                    details: Array.findByField(searchFilter.content, 'id', field),
                    field,
                    value,
                }))
                .filter(({ details }) => details !== undefined)
                .map(({ details }) => (
                    <MagicFilterPill
                        key={`magic-filter-${details.id}`}
                        name={details.label}
                        hasValue
                    >
                        {/* TODO IMPLEMENT DIFFERENT SELECTION TYPES */}
                        {details.label}
                    </MagicFilterPill>
                ))}
            <MagicFilterPill
                name="Name"
            >
                Input Name
            </MagicFilterPill>
            <div className={styles.magicFilter}>
                <AdvancedPopper
                    setIsOpen={setAllFiltersAreOpen}
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
                            disabled={!((filterEntries && filterEntries.size) || searchString)}
                            onClick={handleAllFiltersDelete}
                        >
                            {translations.reset || '???Zurücksetzen???'}
                        </AdvancedPopperAction>
                    )}
                >
                    {searchFilter && (
                        <ul className={styles.filterList}>
                            {searchFilter.content.map(({ id, label }) => (
                                <FilterListEntry
                                    key={`filter-${id}`}
                                    afterSelect={handleAfterSelectFilter}
                                    id={id}
                                    label={label}
                                />
                            ))}
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
