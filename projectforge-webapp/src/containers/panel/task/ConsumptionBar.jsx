import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import { UncontrolledTooltip } from 'reactstrap';
import { Progress } from '../../../components/design';
import style from './ConsumptionBar.module.scss';

function ConsumptionBar({ progress, taskId, identifier }) {
    if (!progress) {
        return <></>;
    }
    const {
        title,
        status,
        percentage,
        id,
    } = progress;

    const element = (
        <>
            <Progress
                value={percentage}
                className={classNames(style.consumption, style[status])}
                id={`${identifier}-${id}`}
            />
            <UncontrolledTooltip placement="right" target={`${identifier}-${id}`}>
                {title}
            </UncontrolledTooltip>
        </>
    );
    if (taskId) {
        return (
            <Link to={`/timesheet?taskId=${taskId}`}>
                {element}
            </Link>
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
    identifier: PropTypes.string,
};

ConsumptionBar.defaultProps = {
    progress: undefined,
    taskId: undefined,
    identifier: 'consumption-bar',
};

export default ConsumptionBar;
