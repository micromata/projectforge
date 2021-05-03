import PropTypes from 'prop-types';
import React from 'react';
import ConsumptionBar from '../../../../../../containers/panel/task/ConsumptionBar';
import { DynamicLayoutContext } from '../../../context';

function CustomizedConsumptionBar() {
    const { variables } = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => {
            if (!variables.task) {
                return <></>;
            }
            const { consumption } = variables.task;
            return (
                <ConsumptionBar
                    progress={consumption}
                    identifier="customized-consumption-bar"
                />
            );
        },
        [variables.task],
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
