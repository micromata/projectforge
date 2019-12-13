import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { getNamedContainer } from '../../../../utilities/layout';
import styles from '../ListPage.module.scss';
import MagicFilterPill from './MagicFilterPill';

function MagicFilters({ searchFilter, translations, filterEntries }) {
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
                        translations={translations}
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
                translations={translations}
            >
                Input Firma
            </MagicFilterPill>
            <MagicFilterPill
                name="Name"
                translations={translations}
            >
                Input Name
            </MagicFilterPill>
            <MagicFilterPill
                name="???Weitere Filter???"
                translations={translations}
            >
                Weitere Filter
            </MagicFilterPill>
        </div>
    );
}

MagicFilters.propTypes = {
    translations: PropTypes.shape({}).isRequired,
    filterEntries: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    searchFilter: PropTypes.shape({}),
};

MagicFilters.defaultProps = {
    searchFilter: {},
};

const mapStateToProps = ({ list }) => {
    const { ui, filter } = list.categories[list.currentCategory];

    return {
        searchFilter: getNamedContainer('searchFilter', ui.namedContainers),
        translations: ui.translations,
        filterEntries: filter.entries,
    };
};

export default connect(mapStateToProps)(MagicFilters);
