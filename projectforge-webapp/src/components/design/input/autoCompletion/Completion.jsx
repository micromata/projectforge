import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './AutoCompletion.module.scss';

function Completion({ displayName, ...props }) {
    return (
        <li
            className={styles.entry}
            role="option"
            aria-selected="false"
            {...props}
        >
            {displayName}
            <FontAwesomeIcon
                icon={faChevronRight}
                className={styles.icon}
            />
        </li>
    );
}

Completion.propTypes = {
    displayName: PropTypes.string.isRequired,
};

Completion.defaultProps = {};

export default Completion;
