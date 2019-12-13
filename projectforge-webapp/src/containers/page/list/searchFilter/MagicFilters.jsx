import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { getNamedContainer } from '../../../../utilities/layout';
import styles from '../ListPage.module.scss';
import MagicFilterPill from './MagicFilterPill';

function MagicFilters({ searchFilter, filterEntries }) {
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
                        value="abc"
                    >
                        {/* TODO IMPLEMENT DIFFERENT SELECTION TYPES */}
                        {details.label}
                    </MagicFilterPill>
                ))}
            <MagicFilterPill
                name="Firma"
                value="Micromata"
            >
                Input Firma
            </MagicFilterPill>
            <MagicFilterPill
                name="Name"
            >
                Input Name
            </MagicFilterPill>
            <MagicFilterPill
                name="???Alle Filter???"
                className={styles.allFilters}
            >
                {searchFilter && (
                    <ul className={styles.filterList}>
                        {searchFilter.content.map(entry => (
                            <li
                                key={`filter-${entry.id}`}
                                className={styles.filter}
                            >
                                {entry.label}
                            </li>
                        ))}
                    </ul>
                )}
            </MagicFilterPill>
        </div>
    );
}

MagicFilters.propTypes = {
    filterEntries: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    searchFilter: PropTypes.shape({}),
};

MagicFilters.defaultProps = {
    searchFilter: undefined,
};

const mapStateToProps = ({ list }) => {
    const { ui, filter } = list.categories[list.currentCategory];

    return {
        searchFilter: getNamedContainer('searchFilter', ui.namedContainers),
        filterEntries: filter.entries,
    };
};

export default connect(mapStateToProps)(MagicFilters);
