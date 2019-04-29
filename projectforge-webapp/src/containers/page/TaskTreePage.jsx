import React from 'react';
import TaskTreePanel from '../panel/task/TaskTreePanel';
import history from '../../utilities/history';

class TaskTreePage extends React.Component {
    static onTaskSelect(id) {
        history.push(`/task/edit/${id}`);
    }

    render() {
        return (
            <TaskTreePanel
                onTaskSelect={TaskTreePage.onTaskSelect}
                showRootForAdmins
                visible
            />
        );
    }
}

export default (TaskTreePage);
