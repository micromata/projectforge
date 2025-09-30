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
    const { field, formatter, dataType } = colDef;
    const { oldDiffValues } = data;
    let oldValue;
    if (oldDiffValues) {
        oldValue = oldDiffValues[field];
    }
    if (oldValue === undefined) {
        return <Formatter {...props} />;
    }
    let useValue = '';
    if (value !== undefined && value !== null) {
        // Apply formatter to new value for consistent formatting with old value
        // Use exact same logic as Formatter.jsx
        const useFormatter = formatter || dataType;
        if (useFormatter) {
            try {
                const formatted = formatterFormat(
                    value,
                    useFormatter,
                    dateFormat,
                    timestampFormatSeconds,
                    timestampFormatMinutes,
                    locale,
                    currency,
                );
                // Ensure result is a string
                useValue = _.isString(formatted) ? formatted : _.toString(value);
            } catch (error) {
                // Fallback to toString if formatting fails
                console.warn('Formatter failed, using toString:', error);
                useValue = _.toString(value);
            }
        } else {
            useValue = _.toString(value);
        }
    }
    return <DiffText newValue={useValue} oldValue={oldValue} />;
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
