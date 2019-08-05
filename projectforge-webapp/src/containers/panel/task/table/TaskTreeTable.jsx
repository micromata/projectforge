import PropTypes from 'prop-types';
import React from 'react';
import { Table } from '../../../../components/design';
import TaskTreeTableEntry from './TaskTreeTableEntry';


function TaskTreeTable(
    {
        columnsVisibility,
        nodes,
        onSelect,
        shortForm,
        translations,
    },
) {
    return (
        <Table striped hover responsive>
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
                        columnsVisibility={columnsVisibility}
                        shortForm={shortForm}
                        task={task}
                    />
                ))}
            </tbody>
        </Table>
    );
}

TaskTreeTable.propTypes = {
    nodes: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    onSelect: PropTypes.func.isRequired,
    columnsVisibility: PropTypes.shape({
        assignedUser: PropTypes.bool,
        kost2: PropTypes.bool,
        orders: PropTypes.bool,
        priority: PropTypes.bool,
        protectionUntil: PropTypes.bool,
        reference: PropTypes.bool,
    }),
    shortForm: PropTypes.bool,
    translations: PropTypes.shape({
        'fibu.auftrag.auftraege': PropTypes.string,
        'fibu.kost2': PropTypes.string,
        priority: PropTypes.string,
        shortDescription: PropTypes.string,
        status: PropTypes.string,
        task: PropTypes.string,
        'task.assignedUser': PropTypes.string,
        'task.consumption': PropTypes.string,
        'task.protectTimesheetsUntil.short': PropTypes.string,
        'task.referece': PropTypes.string,
    }),
};

TaskTreeTable.defaultProps = {
    columnsVisibility: {},
    shortForm: false,
    translations: {},
};

export default TaskTreeTable;
