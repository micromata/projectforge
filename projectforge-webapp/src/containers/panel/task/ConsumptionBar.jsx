import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router';
import { UncontrolledTooltip } from 'reactstrap';
import { Progress } from '../../../components/design';
import style from './ConsumptionBar.module.scss';

function ConsumptionBar({ progress, taskId, identifier = 'consumption-bar' }) {
    if (!progress) {
        return null;
    }
    const {
        title,
        status,
        barPercentage,
        id,
    } = progress;

    const element = (
        <>
            <Progress
                value={barPercentage}
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
        id: PropTypes.number,
        barPercentage: Progress.number,
    }),
    taskId: PropTypes.number,
    identifier: PropTypes.string,
};

export default ConsumptionBar;
