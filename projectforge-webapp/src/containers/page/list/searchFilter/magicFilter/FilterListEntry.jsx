import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { addFilter } from '../../../../../actions/list/filter';
import styles from '../../ListPage.module.scss';

function FilterListEntry(
    {
        id,
        label,
        afterSelect,
        onFilterAdd,
        isSelected,
    },
) {
    const handleSelect = () => {
        if (isSelected) {
            return;
        }

        onFilterAdd(id);
        afterSelect();
    };

    return (
        <li
            className={classNames(styles.filter, { [styles.isSelected]: isSelected })}
            onClick={handleSelect}
            role="option"
            aria-selected="false"
            onKeyPress={undefined}
        >
            {label}
        </li>
    );
}

FilterListEntry.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    afterSelect: PropTypes.func.isRequired,
    onFilterAdd: PropTypes.func.isRequired,
    isSelected: PropTypes.bool.isRequired,
};

FilterListEntry.defaultProps = {};

const mapStateToProps = ({ list }, { id }) => ({
    isSelected: list.categories[list.currentCategory].filter.entries
        .filter(({ field }) => field === id).length !== 0,
});

const actions = (dispatch) => ({
    onFilterAdd: (filterId) => dispatch(addFilter(filterId)),
});

export default connect(mapStateToProps, actions)(FilterListEntry);
