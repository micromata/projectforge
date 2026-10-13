import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './Vacation.module.scss';

function VacationEntries({ entries }) {
    return entries && entries.length > 0 && entries.map((entry, index) => (
        <tr
            key={entry.id}
            className={classNames({ [styles.highlighted]: index === 0 }, { 'ag-row-red': entry.conflict === true })}
            // The form is a page of projectforge-next: a real page load, not client-side routing.
            onClick={() => window.location.assign(`/next/vacation/${entry.id}`)}
        >
            <td>{entry.startDateFormatted}</td>
            <td>{entry.endDateFormatted}</td>
            <td>{entry.statusString}</td>
            <td className={styles.number}>{entry.workingDaysFormatted}</td>
            <td>{entry.specialFormatted}</td>
            <td>{entry.replacement.displayName}</td>
            <td>{entry.otherReplacementsAsString}</td>
            <td>{entry.manager.displayName}</td>
            <td>{entry.vacationModeString}</td>
            <td>{entry.comment}</td>
        </tr>
    ));
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

export default VacationEntries;
