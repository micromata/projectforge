import React from 'react';
import PropTypes from 'prop-types';
import 'moment/min/locales';
import 'react-day-picker/lib/style.css';
import 'rc-time-picker/assets/index.css';
import ReactSelect from '../../ReactSelect';

/**
 * Kost2 selection for editing time sheets.
 */
function TimesheetEditKost2(
    {
        data,
        values,
        changeDataField,
        translations,
    },
) {
    const kost2List = data.task ? data.task.kost2List : undefined;
    if (!Array.isArray(kost2List) || !kost2List.length) {
        return <React.Fragment />;
    }
    return (
        <ReactSelect
            label={translations['fibu.kost2']}
            data={data}
            id={values.id}
            values={kost2List}
            changeDataField={changeDataField}
            translations={translations}
            valueProperty="id"
            labelProperty="title"
        />
    );
}

TimesheetEditKost2.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    values: PropTypes.shape({}).isRequired,
    data: PropTypes.shape({}).isRequired,
    translations: PropTypes.shape({}).isRequired,
};

TimesheetEditKost2.defaultProps = {
};

export default TimesheetEditKost2;
