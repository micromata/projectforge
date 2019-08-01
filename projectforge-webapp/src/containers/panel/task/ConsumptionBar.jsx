import React from 'react';
import PropTypes from 'prop-types';
import { UncontrolledTooltip } from 'reactstrap';
import style from './ConsumptionBar.module.scss';

function ConsumptionBar({ progress, taskId }) {
    if (!progress) {
        return <React.Fragment />;
    }
    const {
        title,
        status,
        width,
        id,
    } = progress;
    const element = (
        <React.Fragment>
            <div className={`${style.progress} ${style[status]}`} id={`cb-${id}`}>
                <div style={{ width }}>
                    {' '}
                </div>
            </div>
            <UncontrolledTooltip placement="right" target={`cb-${id}`}>
                {title}
            </UncontrolledTooltip>
        </React.Fragment>
    );
    if (taskId) {
        return (
            // ToDo: onClick
            <a href={`/timesheet?taskId=${taskId}`}>
                {element}
            </a>
        );
    }
    return element;
}

ConsumptionBar.propTypes = {
    progress: PropTypes.shape({
        title: PropTypes.string,
        status: PropTypes.string,
        width: PropTypes.string,
        id: PropTypes.number,
    }),
    taskId: PropTypes.number,
};

ConsumptionBar.defaultProps = {
    progress: undefined,
    taskId: undefined,
};

export default ConsumptionBar;
