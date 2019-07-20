import { faHome } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { Button } from '../../../../../design';
import inputStyle from '../../../../../design/input/Input.module.scss';
import style from './TaskSelect.module.scss';

function TaskPath(
    {
        path,
        setTask,
    },
) {
    return (
        <div className={style.taskPath}>
            <div className={style.breadcrumb}>
                <Button
                    color="link"
                    onClick={() => setTask(undefined)}
                    style={{ padding: 0 }}
                >
                    <FontAwesomeIcon
                        icon={faHome}
                        className={inputStyle.icon}
                    />
                </Button>
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
};

TaskPath.defaultProps = {
    setTask: undefined,
};

export default TaskPath;
