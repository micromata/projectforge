import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import Formatter from '../../../../components/base/Formatter';
import ConsumptionBar from '../ConsumptionBar';
import TaskTreeContext from '../TaskTreeContext';
import styles from '../TaskTreePanel.module.scss';
import TaskTreeTableEntryNavigation from './TaskTreeTableEntryNavigation';

function TaskTreeTableEntry({ task, consumptionBarClickable }) {
    const {
        columnsVisibility,
        highlightTaskId,
        shortForm,
        selectTask,
    } = React.useContext(TaskTreeContext);

    const { id } = task;

    const handleRowClick = () => selectTask(id, task);

    const kost2TooltipId = task.kost2ListAsLines && task.kost2ListAsLines.length ? `kost2-${task.id}` : undefined;

    return (
        <tr
            onClick={handleRowClick}
            className={classNames(styles.task, { [styles.highlighted]: highlightTaskId === id })}
        >
            <TaskTreeTableEntryNavigation
                treeStatus={task.treeStatus}
                id={id}
                indent={task.indent}
                title={task.title}
            />
            <td>
                <ConsumptionBar
                    progress={task.consumption}
                    taskId={consumptionBarClickable ? id : undefined}
                    identifier="task-tree-entry-consumption-bar"
                />
            </td>

            {columnsVisibility.kost2 && kost2TooltipId && (
                <td>
                    <span id={kost2TooltipId}>
                        {task.kost2WildCard}
                    </span>
                    <UncontrolledTooltip placement="auto" target={kost2TooltipId} style={{ whiteSpace: 'pre-wrap', textAlign: 'left' }}>
                        {task.kost2ListAsLines}
                    </UncontrolledTooltip>

                </td>
            )}
            {columnsVisibility.kost2 && !kost2TooltipId && (
                <td>
                    {task.kost2WildCard}
                </td>
            )}

            {!shortForm && columnsVisibility.orders ? <td>...</td> : undefined}

            <td>{task.shortDescription}</td>

            {!shortForm ? (
                <>

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

                </>
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
        reference: PropTypes.string,
        priority: PropTypes.string,
        kost2WildCard: PropTypes.string,
        kost2ListAsLines: PropTypes.string,
    }).isRequired,
    /* If clickable a click on the consumption bar redirects to task view. */
    consumptionBarClickable: PropTypes.bool,
};

TaskTreeTableEntry.defaultProps = {
    consumptionBarClickable: undefined,
};

export default TaskTreeTableEntry;
