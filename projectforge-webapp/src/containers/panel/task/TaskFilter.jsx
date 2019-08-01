import { faSearch } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, CheckBox, Col, Input, Row } from '../../../components/design';
import style from './TaskTreePanel.module.scss';

function TaskFilter(
    {
        onChange: handleSearchChange,
        onCheckBoxChange: handleCheckBoxChange,
        onSubmit: handleSubmitButton,
        filter,
        translations,
    },
) {
    const [isOpen, setIsOpen] = React.useState(false);
    const reference = React.useRef(undefined);
    const basicReference = React.useRef(undefined);

    const handleMouseClick = (event) => {
        if (reference.current && !reference.current.contains(event.target)) {
            setIsOpen(false);
        }
    };

    const handleSubmitButtonClick = (event) => {
        setIsOpen(false);
        handleSubmitButton(event);
    };

    const handleInputKeyPress = (event) => {
        if (event.key === 'Enter') {
            event.target.blur();
            handleSubmitButtonClick(event);
        }
    };

    React.useEffect(() => {
        // Register accessibility listeners when search is open.
        if (isOpen) {
            document.addEventListener('click', handleMouseClick);
        }

        // Remove accessibility listeners when search is closed.
        return () => document.removeEventListener('click', handleMouseClick);
    }, [isOpen]);

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

        if (additionalFilters.length) {
            if (searchString) {
                searchValue += ' | ';
            }

            searchValue += additionalFilters.join(', ');
        }
    }

    return (
        <div className={classNames(style.searchContainer, { [style.isOpen]: isOpen })}>
            <div
                style={{ height: `${(basicReference.current || {}).clientHeight || 0}px` }}
            />
            <div
                ref={reference}
                className={classNames(style.search, { [style.isOpen]: isOpen })}
            >
                <div className={style.basic} ref={basicReference} onFocus={() => setIsOpen(true)}>
                    <Row>
                        <Col sm={10}>
                            <Input
                                label={translations.searchFilter || ''}
                                id="taskSearchString"
                                value={searchValue}
                                onChange={handleSearchChange}
                                autoComplete="off"
                                className={style.searchString}
                                onKeyPress={handleInputKeyPress}
                            />
                        </Col>
                        <Col sm={2}>
                            <Button
                                color="primary"
                                onClick={handleSubmitButtonClick}
                                type="button"
                                size="sm"
                            >
                                <FontAwesomeIcon icon={faSearch} />
                            </Button>
                        </Col>
                    </Row>
                </div>
                <Row className={style.advanced}>
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
            </div>
        </div>
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

TaskFilter.defaultProps = {
    translations: {},
};

export default TaskFilter;
