import React from 'react';

export const taskTreeContextDefaultValues = {
    /**
     * type {Number} Id of the task that should be highlighted.
     */
    highlightTaskId: undefined,
    /**
     * @type {Object} Translations that are shown in the TaskTree
     */
    translations: {
        'fibu.auftrag.auftraege': 'Orders',
        'fibu.kost2': 'Cost2',
        priority: 'Priority',
        shortDescription: 'Short description',
        status: 'Status',
        task: 'Structure element',
        'task.assignedUser': 'Responsible user',
        'task.consumption': 'Consumption',
        'task.protectTimesheetsUntil.short': 'Protected until',
        'task.reference': 'Reference',
    },
};

const TaskTreeContext = React.createContext(taskTreeContextDefaultValues);

export default TaskTreeContext;
