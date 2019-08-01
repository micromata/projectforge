import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../../context';
import ConsumptionBar from '../../../../../../containers/panel/task/ConsumptionBar';

function TimesheetTemplatesAndRecents() {
    const { variables } = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => {
            // Ignore task id to prevent clickable consumption bar.
            return (
                <React.Fragment>
                    Templates | Recents
                </React.Fragment>
            );
        },
        [variables.task.consumption],
    );
}

TimesheetTemplatesAndRecents.propTypes = {
};

TimesheetTemplatesAndRecents.defaultProps = {};

export default TimesheetTemplatesAndRecents;
