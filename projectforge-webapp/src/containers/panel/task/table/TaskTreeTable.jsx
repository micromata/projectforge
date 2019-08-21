import PropTypes from 'prop-types';
import React from 'react';
import { Table } from '../../../../components/design';
import TaskTreeContext from '../TaskTreeContext';
import styles from '../TaskTreePanel.module.scss';
import TaskTreeTableEntry from './TaskTreeTableEntry';


function TaskTreeTable({ nodes }) {
    const {
        columnsVisibility,
        shortForm,
        translations,
    } = React.useContext(TaskTreeContext);

    return (
        <Table striped hover responsive className={styles.tasks}>
            <thead>
                <tr>
                    <th>{translations.task}</th>
                    <th>{translations['task.consumption']}</th>

                    {columnsVisibility.kost2 ? <th>{translations['fibu.kost2']}</th> : undefined}

                    {!shortForm && columnsVisibility.orders
                        ? <th>{translations['fibu.auftrag.auftraege']}</th>
                        : undefined}

                    <th>{translations.shortDescription}</th>

                    {!shortForm ? (
                        <React.Fragment>

                            {columnsVisibility.protectionUntil
                                ? <th>{translations['task.protectTimesheetsUntil.short']}</th>
                                : undefined}

                            {columnsVisibility.reference
                                ? <th>{translations['task.reference']}</th>
                                : undefined}

                            {columnsVisibility.priority
                                ? <th>{translations.priority}</th>
                                : undefined}

                            <th>{translations.status}</th>

                            {columnsVisibility.assignedUser
                                ? <th>{translations['task.assignedUser']}</th>
                                : undefined}

                        </React.Fragment>
                    ) : undefined}
                </tr>
            </thead>
            <tbody>
                {nodes.map(task => (
                    <TaskTreeTableEntry
                        key={`task-tree-table-body-row-${task.id}`}
                        task={task}
                    />
                ))}
            </tbody>
        </Table>
    );
}

TaskTreeTable.propTypes = {
    nodes: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
};

export default TaskTreeTable;
