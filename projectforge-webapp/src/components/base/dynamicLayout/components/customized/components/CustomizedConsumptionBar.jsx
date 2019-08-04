import PropTypes from 'prop-types';
import React from 'react';
import { DynamicLayoutContext } from '../../../context';
import ConsumptionBar from '../../../../../../containers/panel/task/ConsumptionBar';

function CustomizedConsumptionBar() {
    const { variables } = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => {
            const { consumption } = variables.task;
            const { taskId } = variables;
            // Ignore task id to prevent clickable consumption bar.
            return (
                <ConsumptionBar
                    progress={consumption}
                    taskId={taskId}
                />
            );
        },
        [variables.task.consumption],
    );
}

CustomizedConsumptionBar.propTypes = {
    progress: PropTypes.shape({
        title: PropTypes.string,
        status: PropTypes.string,
        width: PropTypes.string,
        id: PropTypes.number,
    }),
    taskId: PropTypes.number,
};

CustomizedConsumptionBar.defaultProps = {
    progress: undefined,
    taskId: undefined,
};

export default CustomizedConsumptionBar;
