import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../../../../utilities/rest';
import inputStyle from '../../../../../design/input/Input.module.scss';
import { DynamicLayoutContext } from '../../../context';
import TaskPath from './TaskPath';

function DynamicTaskSelect(
    {
        id,
        label,
        onKost2Changed,
        variables,
    },
) {
    const { setData } = React.useContext(DynamicLayoutContext);

    const [panelVisibile, setPanelVisible] = React.useState(false);
    const [modalHighlight, setModalHighlight] = React.useState(undefined);
    const [task, setStateTask] = React.useState(undefined);
    const panelRef = React.useRef(null);

    // Handle mouse events
    React.useEffect(() => {
        const handleClickOutside = ({ target }) => {
            if (panelRef && !panelRef.current.contains(target)) {
                setPanelVisible(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);

        setStateTask(variables.task);

        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const setTask = (taskId, selectedTask) => {
        if (selectedTask) {
            setPanelVisible(false);
        }

        if (!taskId) {
            setStateTask(undefined);
            setData({ [id]: undefined });

            // Emit onKost2Changed handler if defined.
            if (onKost2Changed) {
                onKost2Changed();
            }

            setModalHighlight(undefined);

            return;
        }

        fetch(
            getServiceURL(`task/info/${taskId}`),
            {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json',
                },
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then((json) => {
                setStateTask(json);

                if (json) {
                    setData({ [id]: { id: json.id } });

                    if (onKost2Changed) {
                        onKost2Changed(json.kost2List);
                    }

                    setStateTask(json);
                }
            });
    };

    const toggleModal = () => {
        setPanelVisible(!panelVisibile);
        setModalHighlight(undefined); // Reset to highlight current task.
    };

    // Opens the task tree modal dialog with the given task highlighted.
    const openModal = (taskId) => {
        setPanelVisible(true);
        setModalHighlight(taskId); // Highlight selected ancestor task.
    };

    return (
        <div>
            {task
                ? <TaskPath path={[...task.path, task]} />
                : <span className={inputStyle.text}>{label}</span>}
        </div>
    );
}

DynamicTaskSelect.propTypes = {};

DynamicTaskSelect.defaultProps = {};

export default DynamicTaskSelect;
