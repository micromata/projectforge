import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../../context';
import ConsumptionBar from '../../../../../../containers/panel/task/ConsumptionBar';

function CustomizedConsumptionBar() {
    const { variables } = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => {
            const { consumption } = variables.task;
            // Ignore task id to prevent clickable consumption bar.
            return (
                <ConsumptionBar
                    progress={consumption}
                />
            );
        },
        [variables.task.consumption],
    );
}

CustomizedConsumptionBar.propTypes = {
    user: PropTypes.shape({}).isRequired,
};

CustomizedConsumptionBar.defaultProps = {};

const mapStateToProps = state => ({
    user: state.authentication.user,
});


export default connect(mapStateToProps)(CustomizedConsumptionBar);
