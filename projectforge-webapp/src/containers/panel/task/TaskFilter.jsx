import { faSearch } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { CheckBox, Col, Row } from '../../../components/design';
import AdvancedPopper from '../../../components/design/popper/AdvancedPopper';
import AdvancedPopperInput from '../../../components/design/popper/AdvancedPopperInput';
import TaskTreeContext from './TaskTreeContext';
import style from './TaskTreePanel.module.scss';

function TaskFilter(
    {
        onChange: handleSearchChange,
        onCheckBoxChange: handleCheckBoxChange,
        onSubmit: handleSubmitButton,
        filter,
    },
) {
    const { translations } = React.useContext(TaskTreeContext);
    const [isOpen, setIsOpen] = React.useState(false);

    const handleInputKeyPress = (event) => {
        if (event.key === 'Enter') {
            event.target.blur();
            setIsOpen(false);
            handleSubmitButton(event);
            event.preventDefault();
        }
    };

    const {
        searchString,
        opened,
        notOpened,
        closed,
        deleted,
    } = filter;

    let searchValue = searchString;

    if (!isOpen) {
        const additionalFilters = [];

        if (!opened || !notOpened || closed || deleted) {
            // Not standard filter
            if (opened) {
                additionalFilters.push(translations['task.status.opened']);
            }
            if (notOpened) {
                additionalFilters.push(translations['task.status.notOpened']);
            }
            if (closed) {
                additionalFilters.push(translations['task.status.closed']);
            }
            if (deleted) {
                additionalFilters.push(translations.deleted);
            }
        }

        if (additionalFilters.length) {
            if (searchString) {
                searchValue += ' | ';
            }

            searchValue += additionalFilters.join(', ');
        }
    }

    return (
        <AdvancedPopper
            basic={(
                <AdvancedPopperInput
                    icon={faSearch}
                    id="taskSearchString"
                    onChange={handleSearchChange}
                    onKeyPress={handleInputKeyPress}
                    value={searchValue}
                    noStyle
                />
            )}
            className={style.searchContainer}
            isOpen={isOpen}
            setIsOpen={setIsOpen}
        >
            <Row className={style.additional}>
                <Col sm={6}>
                    <CheckBox
                        id="opened"
                        label={translations['task.status.opened']}
                        onChange={handleCheckBoxChange}
                        checked={opened}
                    />
                </Col>
                <Col sm={6}>
                    <CheckBox
                        id="notOpened"
                        label={translations['task.status.notOpened']}
                        onChange={handleCheckBoxChange}
                        checked={notOpened}
                    />
                </Col>
                <Col sm={6}>
                    <CheckBox
                        id="closed"
                        label={translations['task.status.closed']}
                        onChange={handleCheckBoxChange}
                        checked={closed}
                    />
                </Col>
                <Col sm={6}>
                    <CheckBox
                        id="deleted"
                        label={translations.deleted}
                        onChange={handleCheckBoxChange}
                        checked={deleted}
                    />
                </Col>
            </Row>
        </AdvancedPopper>
    );
}

TaskFilter.propTypes = {
    filter: PropTypes.shape({
        searchString: PropTypes.string.isRequired,
        opened: PropTypes.bool,
        notOpened: PropTypes.bool,
        closed: PropTypes.bool,
        deleted: PropTypes.bool,
    }).isRequired,
    onChange: PropTypes.func.isRequired,
    onCheckBoxChange: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        searchFilter: PropTypes.string,
        search: PropTypes.string,
        'task.status.opened': PropTypes.string,
        'task.status.notOpened': PropTypes.string,
        'task.status.closed': PropTypes.string,
        deleted: PropTypes.string,
    }),
};

export default TaskFilter;
