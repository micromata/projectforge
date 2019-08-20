import React from 'react';

const notImplementedFunction = () => {
    throw Error('Not implemented yet.');
};

export const taskTreeContextDefaultValues = {
    /**
     * @type {Object} Which columns should be visible for the current user.
     */
    columnsVisibility: {
        assignedUser: false,
        kost2: false,
        orders: false,
        priority: false,
        protectionUntil: false,
        reference: false,
    },
    /**
     * Handle the task selection.
     *
     * @param {Number} taskId The id of the selected Task
     * TODO: Is the selectedTask argument necessary?
     * @param {Object} selectedTask The selected task.
     */
    selectTask: (taskId, selectedTask) => notImplementedFunction(taskId, selectedTask),
    /**
     * @type {Boolean} Should the table be shown in a shorter form. Not all fields will be visible.
     */
    shortForm: false,
    /**
     * Open/Close a task folder.
     *
     * @param taskId The id of the task to toggle.
     * @param to The state it should switch from. "OPENED" or "CLOSED".
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
