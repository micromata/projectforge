import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import history from '../../../../../../../utilities/history';
import prefix from '../../../../../../../utilities/prefix';
import styles from './Vacation.module.scss';

function VacationEntries({ entries }) {
    return (
        <>
            {entries && entries.length > 0 && entries.map((entry, index) => (
                <tr
                    key={entry.id}
                    className={classNames({ [styles.highlighted]: index === 0 })}
                    onClick={() => history.push(`${prefix}vacation/edit/${entry.id}?returnToCaller=account`)}
                >
                    <td>{entry.startDateFormatted}</td>
                    <td>{entry.endDateFormatted}</td>
                    <td>{entry.statusString}</td>
                    <td className={styles.number}>{entry.workingDaysFormatted}</td>
                    <td>{entry.specialFormatted}</td>
                    <td>{entry.replacement.displayName}</td>
                    <td>{entry.manager.displayName}</td>
                    <td>{entry.vacationModeString}</td>
                    <td>{entry.comment}</td>
                </tr>
            ))}
        </>
    );
}

VacationEntries.propTypes = {
    entries: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
        startDateFormatted: PropTypes.string,
        endDateFormatted: PropTypes.string,
        statusString: PropTypes.string,
        workingDaysFormatted: PropTypes.string,
        specialFormatted: PropTypes.string,
        replacement: PropTypes.shape({
            displayName: PropTypes.string,
        }),
        manager: PropTypes.shape({
            displayName: PropTypes.string,
        }),
        vacationModeString: PropTypes.string,
        comment: PropTypes.string,
    })),
};

VacationEntries.defaultProps = {
    entries: undefined,
};

export default VacationEntries;
