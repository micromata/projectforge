import React from 'react';

const notImplementedFunction = () => {
    throw Error('Not implemented yet.');
};

export const taskTreeContextDefaultValues = {
    /**
     * type {Number} Id of the task that should be highlighted.
     */
    highlightTaskId: undefined,
    /**
     * Open/Close a task folder.
     *
     * @param taskId The id of the task to toggle.
     * @param from The state it should switch from. "OPENED" or "CLOSED".
     */
    toggleTask: (taskId, from) => notImplementedFunction(taskId, from),
    /**
     * @type {Object} Translations that are shown in the TaskTree
     */
    translations: {
        'fibu.auftrag.auftraege': '???',
        'fibu.kost2': '???',
        priority: '???',
        shortDescription: '???',
        status: '???',
        task: '???',
        'task.assignedUser': '???',
        'task.consumption': '???',
        'task.protectTimesheetsUntil.short': '???',
        'task.reference': '???',
    },
};

const TaskTreeContext = React.createContext(taskTreeContextDefaultValues);

export default TaskTreeContext;
