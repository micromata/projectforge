import { faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import PropTypes from 'prop-types';
import style from '../../../design/input/Input.module.scss';

class TaskSelect extends React.Component {
    constructor(props) {
        super(props);

        this.setParentTask = this.setParentTask.bind(this);
    }

    setParentTask(task, ancestorId) {
        const { id, changeDataField } = this.props;
        const newTask = {
            id: ancestorId,
            path: [],
        };
        let finished;
        task.path.map((ancestor) => {
            if (ancestor.id === ancestorId) {
                finished = true;
                newTask.title = ancestor.title;
            }
            if (!finished) {
                newTask.path.push({
                    id: ancestor.id,
                    title: ancestor.title,
                });
            }
        });
        console.log(newTask)
        changeDataField(id, newTask);
    }

    render() {
        const { label, data, id } = this.props;
        const task = Object.getByString(data, id);
        let recentAncestor;
        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                {
                    task.path.map((ancestor) => {
                        let removeLink;
                        if (recentAncestor) {
                            const parentTaskId = recentAncestor;
                            removeLink = (
                                <a onClick={() => this.setParentTask(task, parentTaskId)}>
                                    <FontAwesomeIcon icon={faTimesCircle} className={style.icon}/>
                                </a>);
                        }
                        recentAncestor = ancestor.id;
                        return (
                            <React.Fragment key={ancestor.id}>
                                <a href={`/task/edit/${ancestor.id}`}>
                                    {ancestor.title}
                                </a>
                                {' '}
                                {removeLink}
                                <span style={{
                                    fontWeight: 'bold',
                                    color: 'red',
                                    fontSize: '1.2em',
                                }}
                                >
                                    {' | '}
                                </span>
                            </React.Fragment>);
                    })}
                <a href={`/task/edit/${task.id}`}>
                    {task.title}
                </a>
                {' '}
                {(() => {
                    if (recentAncestor) {
                        return (
                            <a onClick={() => this.setParentTask(task, recentAncestor)}>
                                <FontAwesomeIcon icon={faTimesCircle} className={style.icon} />
                            </a>
                        );
                    }
                    return '';
                })()}
            </React.Fragment>
        );
    }
}

TaskSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

export default TaskSelect;
