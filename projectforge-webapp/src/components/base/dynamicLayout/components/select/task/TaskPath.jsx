import { faHome } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import { Button } from '../../../../../design';
import inputStyle from '../../../../../design/input/Input.module.scss';
import style from './TaskSelect.module.scss';

function TaskPath(
    {
        path,
        setTask,
        translations,
    },
) {
    return (
        <div className={style.taskPath}>
            <div className={style.breadcrumb}>
                <Button
                    id="rootTask"
                    color="link"
                    onClick={() => setTask(undefined)}
                    style={{ padding: 0 }}
                >
                    <FontAwesomeIcon
                        icon={faHome}
                        className={inputStyle.icon}
                    />
                </Button>
                <UncontrolledTooltip placement="bottom" target="rootTask">
                    {translations['task.tree.rootNode']}
                </UncontrolledTooltip>
            </div>
            {path.map(({ id, title }) => (
                <div
                    role="link"
                    tabIndex={0}
                    key={id}
                    className={style.breadcrumb}
                    onClick={() => setTask(id)}
                    onKeyPress={() => setTask(id)}
                >
                    {title}
                </div>
            ))}
        </div>
    );
}

TaskPath.propTypes = {
    path: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
        title: PropTypes.string.isRequired,
    })).isRequired,
    setTask: PropTypes.func,
    translations: PropTypes.shape({
        'task.tree.rootNode': PropTypes.string.isRequired,
    }).isRequired,
};

export default TaskPath;
