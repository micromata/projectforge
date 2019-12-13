import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { openEditPage } from '../../../../actions';
import styles from '../ListPage.module.scss';

function QuickSelectionEntry({ displayName, onClick }) {
    return (
        <li
            className={styles.entry}
            onClick={onClick}
            role="option"
            aria-selected="false"
            onKeyPress={undefined}
        >
            {displayName}
            <FontAwesomeIcon
                icon={faChevronRight}
                className={styles.icon}
            />
        </li>
    );
}

QuickSelectionEntry.propTypes = {
    displayName: PropTypes.string.isRequired,
    // id is used by redux.
    // eslint-disable-next-line react/no-unused-prop-types
    id: PropTypes.number.isRequired,
    onClick: PropTypes.func.isRequired,
};

QuickSelectionEntry.defaultProps = {};

const mapStateToProps = undefined;

const actions = (dispatch, { id }) => ({
    onClick: () => dispatch(openEditPage(id)),
});

export default connect(mapStateToProps, actions)(QuickSelectionEntry);
