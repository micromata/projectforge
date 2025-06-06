import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import DiffText from '../../../../design/DiffText';
import Formatter from '../../../Formatter';

function DynamicAgGridDiffCell(props) {
    const {
        value,
        colDef,
        data,
    } = props;
    const { field } = colDef;
    const { oldDiffValues } = data;
    let oldValue;
    if (oldDiffValues) {
        oldValue = oldDiffValues[field];
    }
    if (oldValue === undefined) {
        return <Formatter {...props} />;
    }
    let useValue = '';
    if (value) {
        useValue = _.toString(value);
    }
    return <DiffText newValue={useValue} oldValue={oldValue} />;
}

DynamicAgGridDiffCell.propTypes = {
    // eslint-disable-next-line react/forbid-prop-types
    value: PropTypes.any, // string, number, boolean, array, ...
    colDef: PropTypes.shape(),
    data: PropTypes.shape(),
};

export default (DynamicAgGridDiffCell);
