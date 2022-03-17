import PropTypes from 'prop-types';
import React from 'react';
import { Table } from '../../../../components/design';
import TaskTreeContext from '../TaskTreeContext';
import styles from '../TaskTreePanel.module.scss';
import TaskTreeTableEntry from './TaskTreeTableEntry';

function TaskTreeTable({ nodes, consumptionBarClickable }) {
    const {
        columnsVisibility,
        shortForm,
        translations,
    } = React.useContext(TaskTreeContext);

    return (
        <>
            <Table striped hover responsive className={styles.tasks}>
                <thead>
                    <tr>
                        <th>{translations.task}</th>
                        <th>{translations['task.consumption']}</th>

                        {columnsVisibility.kost2 && <th>{translations['fibu.kost2']}</th>}

                        {!shortForm && columnsVisibility.orders
                        && <th>{translations['fibu.auftrag.auftraege']}</th>}

                        <th>{translations.shortDescription}</th>

                        {!shortForm && (
                            <>
                                {columnsVisibility.protectionUntil
                                && <th>{translations['task.protectTimesheetsUntil.short']}</th>}

                                {columnsVisibility.reference
                                && <th>{translations['task.reference']}</th>}

                                {columnsVisibility.priority
                                && <th>{translations.priority}</th>}

                                <th>{translations.status}</th>

                                {columnsVisibility.assignedUser
                                && <th>{translations['task.assignedUser']}</th>}

                            </>
                        )}
                    </tr>
                </thead>
                <tbody>
                    {nodes.map((task) => (
                        <TaskTreeTableEntry
                            key={`task-tree-table-body-row-${task.id}`}
                            task={task}
                            consumptionBarClickable={consumptionBarClickable}
                        />
                    ))}
                </tbody>
            </Table>
            {/* TODO TRANSLATION */}
            {nodes.length === 0 && <span>[Keine Tasks gefunden]</span>}
        </>
    );
}

TaskTreeTable.propTypes = {
    nodes: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    consumptionBarClickable: PropTypes.bool,
};

TaskTreeTable.defaultProps = {
    consumptionBarClickable: undefined,
};

export default TaskTreeTable;
