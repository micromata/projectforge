import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import styles from './Vacation.module.scss';
import { VacationStatisticsContext } from './VacationStatisticsContext';

function VacationStatisticsEntry(
    {
        field,
        highlighted,
        isHead,
        title,
    },
) {
    const { current, prev, translations } = React.useContext(VacationStatisticsContext);

    const currentField = current[field];
    const prevField = prev[field];

    if (!(currentField || prevField)) {
        return <></>;
    }

    const Tag = isHead ? 'th' : 'td';

    return (
        <tr className={classNames({ [styles.highlighted]: highlighted })}>
            <Tag>{translations[title]}</Tag>
            <Tag className={styles.number}>{currentField}</Tag>
            <Tag className={styles.number}>{prevField}</Tag>
        </tr>
    );
}

VacationStatisticsEntry.propTypes = {
    field: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    highlighted: PropTypes.bool,
    isHead: PropTypes.bool,
};

VacationStatisticsEntry.defaultProps = {
    highlighted: false,
    isHead: false,
};

export default VacationStatisticsEntry;
