import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import ReadonlyField from '../../../../design/input/ReadonlyField';
import formatterFormat from '../../../FormatterFormat';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

/**
 * ReadonlyField text (with label and optional tooltip)
 */
function DynamicReadonlyField(
    {
        id,
        dataType,
        value,
        locale,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    let dataValue = value || Object.getByString(data, id) || '';

    if (dataType && dataType !== 'STRING' && dataType !== 'BOOLEAN' && dataValue !== '') {
        const formatted = formatterFormat(dataValue, dataType, null, null, null, locale, null);
        if (formatted !== undefined && formatted !== null) {
            dataValue = formatted;
        }
    }

    return React.useMemo(() => (
        <DynamicValidationManager id={id}>
            <ReadonlyField
                id={`${ui.uid}-${id}`}
                {...props}
                value={dataValue}
                dataType={dataType}
            />
        </DynamicValidationManager>
    ), [dataValue, setData, id, dataType, props]);
}

DynamicReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
    dataType: PropTypes.string,
    locale: PropTypes.string,
};

const mapStateToProps = ({ authentication }) => ({
    locale: authentication?.user?.locale,
});

export default connect(mapStateToProps)(DynamicReadonlyField);
