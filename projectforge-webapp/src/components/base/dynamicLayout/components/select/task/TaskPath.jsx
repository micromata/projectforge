import { faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { Button } from '../../../../../design';
import inputStyle from '../../../../../design/input/Input.module.scss';
import style from './TaskSelect.module.scss';
import TaskTitle from './TaskTitle';

function TaskPath(
    {
        modalHighlight,
        openModal,
        path,
        setTask,
    },
) {
    return React.useMemo(() => {
        let recentAncecstorId;

        return (
            <React.Fragment>
                {path.map((ancestor) => {
                    const parenTaskId = recentAncecstorId;
                    recentAncecstorId = ancestor.id;

                    return (
                        <React.Fragment key={ancestor.id}>
                            <TaskTitle
                                id={ancestor.id}
                                isHighlighted={ancestor.id === modalHighlight}
                                title={ancestor.title}
                                openModal={openModal}
                            />
                            {' '}
                            <Button
                                color="link"
                                onClick={() => setTask(parenTaskId)}
                                style={{ padding: 0 }}
                            >
                                <FontAwesomeIcon
                                    icon={faTimesCircle}
                                    className={inputStyle.icon}
                                    color="lightGray"
                                />
                            </Button>
                            <span className={style.divider}>
                                {' | '}
                            </span>
                        </React.Fragment>
                    );
                })}
            </React.Fragment>
        );
    }, [modalHighlight, openModal, path, setTask]);
}

TaskPath.propTypes = {
    path: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
        title: PropTypes.string.isRequired,
    })).isRequired,
    modalHighlight: PropTypes.number,
    openModal: PropTypes.func,
    setTask: PropTypes.func,
};

TaskPath.defaultProps = {
    modalHighlight: undefined,
    openModal: undefined,
    setTask: undefined,
};

export default TaskPath;
