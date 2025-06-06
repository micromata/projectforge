import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './AutoCompletion.module.scss';

function Completion({ displayName, selected = false, ...props }) {
    return (
        <li
            className={classNames(styles.entry, { [styles.selected]: selected })}
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
    selected: PropTypes.bool,
};

export default Completion;
