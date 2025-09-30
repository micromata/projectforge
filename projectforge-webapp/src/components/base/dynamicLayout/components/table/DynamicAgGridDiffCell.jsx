import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { connect } from 'react-redux';
import DiffText from '../../../../design/DiffText';
import Formatter from '../../../Formatter';
import formatterFormat from '../../../FormatterFormat';

function DynamicAgGridDiffCell(props) {
    const {
        value,
        colDef,
        data,
        locale,
        currency,
        dateFormat = 'DD/MM/YYYY',
        timestampFormatSeconds = 'DD.MM.YYYY HH:mm:ss',
        timestampFormatMinutes = 'DD.MM.YYYY HH:mm',
    } = props;
    const { field, cellRendererParams } = colDef;
    // Formatter is stored in cellRendererParams.dataType by backend
    const formatter = cellRendererParams?.formatter;
    const dataType = cellRendererParams?.dataType;
    const { oldDiffValues } = data;
    let oldValueRaw;
    if (oldDiffValues) {
        oldValueRaw = oldDiffValues[field];
    }
    if (oldValueRaw === undefined) {
        return <Formatter {...props} />;
    }

    const useFormatter = formatter || dataType;

    // Helper function to format a value
    const formatValue = (val) => {
        if (val === undefined || val === null) {
            return '';
        }

        // If we have a formatter, use it (for numbers, dates, etc.)
        if (useFormatter) {
            // Only skip formatting for string values that are NOT numeric strings
            // (e.g., kreditor, betreff, etc. should not be formatted)
            if (_.isString(val) && Number.isNaN(Number(val))) {
                // This is a text string (not a number), return as-is
                return val;
            }
            // This is a number or numeric string, format it
            try {
                const formatted = formatterFormat(
                    val,
                    useFormatter,
                    dateFormat,
                    timestampFormatSeconds,
                    timestampFormatMinutes,
                    locale,
                    currency,
                );
                return _.isString(formatted) ? formatted : _.toString(val);
            } catch (error) {
                console.warn('Formatter failed for value:', val, 'formatter:', useFormatter, error);
                return _.toString(val);
            }
        }

        // No formatter, just convert to string
        return _.toString(val);
    };

    // Format both old and new values identically
    const formattedOldValue = formatValue(oldValueRaw);
    const formattedNewValue = formatValue(value);

    return <DiffText newValue={formattedNewValue} oldValue={formattedOldValue} />;
}

DynamicAgGridDiffCell.propTypes = {
    // eslint-disable-next-line react/forbid-prop-types
    value: PropTypes.any, // string, number, boolean, array, ...
    colDef: PropTypes.shape(),
    data: PropTypes.shape(),
    locale: PropTypes.string,
    currency: PropTypes.string,
    dateFormat: PropTypes.string,
    timestampFormatSeconds: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
};

DynamicAgGridDiffCell.defaultProps = {
    locale: undefined,
    currency: undefined,
    dateFormat: 'DD/MM/YYYY',
    timestampFormatSeconds: 'DD.MM.YYYY HH:mm:ss',
    timestampFormatMinutes: 'DD.MM.YYYY HH:mm',
};

const mapStateToProps = ({ authentication }) => ({
    locale: authentication.user.locale,
    currency: authentication.user.currency,
    dateFormat: authentication.user.jsDateFormat,
    timestampFormatSeconds: authentication.user.jsTimestampFormatSeconds,
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(DynamicAgGridDiffCell);
