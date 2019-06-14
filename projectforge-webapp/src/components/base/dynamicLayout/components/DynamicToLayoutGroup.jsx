import React from 'react';
import LayoutGroup from '../../page/layout/LayoutGroup';
import { DynamicLayoutContext } from '../context';

/*
 * This component is a bridge between the new DynamicLayout and the old LayoutGroup. Components
 * that aren't rebuild with the new DynamicLayout system can be used.
 */
function DynamicToLayoutGroup(props) {
    // Load context
    const {
        data,
        ui,
        setData,
        validation,
    } = React.useContext(DynamicLayoutContext);

    // Map the changeDataField method to the new setData method.
    const changeDataField = (id, newValue) => {
        let newData = newValue;

        if (newValue.id) {
            newData = { id: newValue.id };
        }

        if (newValue.value) {
            newData = newValue.value;
        }

        setData({ [id]: newData });
    };

    // Return a LayoutGroup where the old system will be rendered.
    return (
        <LayoutGroup
            content={[props]}
            changeDataField={changeDataField}
            data={data}
            translations={ui.translations}
            validation={validation}
        />
    );
}

DynamicToLayoutGroup.propTypes = {};

DynamicToLayoutGroup.defaultProps = {};

export default DynamicToLayoutGroup;
