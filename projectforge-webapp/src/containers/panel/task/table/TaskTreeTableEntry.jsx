import PropTypes from 'prop-types';
import React from 'react';
import Formatter from '../../../../components/base/Formatter';
import ConsumptionBar from '../ConsumptionBar';
import TaskTreeTableEntryIcon from './TaskTreeTableEntryIcon';

function TaskTreeTableEntry({ task, columnsVisibility, shortForm }) {
    return (
        <tr>
            <td style={{ paddingLeft: `${task.indent * 1.5 + 0.75}rem` }}>
                <TaskTreeTableEntryIcon treeStatus={task.treeStatus} />
                {task.title}
            </td>
            <td>
                <ConsumptionBar
                    progress={task.consumption}
                    taskId={task.id}
                    identifier="task-tree-entry-consumption-bar"
                />
            </td>

            {columnsVisibility.kost2 ? <td>...</td> : undefined}

            {!shortForm && columnsVisibility.orders ? <td>...</td> : undefined}

            <td>{task.shortDescription}</td>

            {!shortForm ? (
                <React.Fragment>

                    {columnsVisibility.protectionUntil ? (
                        <td>
                            <Formatter
                                formatter="DATE"
                                data={task.protectTimesheetsUntil}
                                id="date"
                            />
                        </td>
                    ) : undefined}

                    {columnsVisibility.reference ? <td>{task.reference}</td> : undefined}

                    {columnsVisibility.priority ? <td>{task.priority}</td> : undefined}

                    <td>{task.status}</td>

                    {columnsVisibility.assignedUser ? (
                        <td>{task.responsibleUser ? task.responsibleUser.fullname : ''}</td>
                    ) : undefined}

                </React.Fragment>
            ) : undefined}
        </tr>
    );
}

TaskTreeTableEntry.propTypes = {
    task: PropTypes.shape({
        id: PropTypes.number.isRequired,
        indent: PropTypes.number.isRequired,
        title: PropTypes.string.isRequired,
        treeStatus: PropTypes.oneOf(['OPENED', 'CLOSED', 'LEAF']).isRequired,
        consumption: PropTypes.shape({}),
        protectTimesheetsUntil: PropTypes.string,
        responsibleUser: PropTypes.shape({
            fullname: PropTypes.string,
        }),
        shortDescription: PropTypes.string,
        status: PropTypes.string,
    }).isRequired,
    columnsVisibility: PropTypes.shape({
        assignedUser: PropTypes.bool,
        kost2: PropTypes.bool,
        orders: PropTypes.bool,
        priority: PropTypes.bool,
        protectionUntil: PropTypes.bool,
        reference: PropTypes.bool,
    }),
    shortForm: PropTypes.bool,
};

TaskTreeTableEntry.defaultProps = {
    columnsVisibility: {},
    shortForm: false,
};

export default TaskTreeTableEntry;
