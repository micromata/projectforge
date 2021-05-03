import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { debouncedWaitTime, getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import AdvancedPopper from '../../popper/AdvancedPopper';
import styles from './AutoCompletion.module.scss';
import Completion from './Completion';

const loadCompletionsBounced = (
    {
        url,
        search = '',
        setCompletions,
        searchParameter,
        signal,
    },
) => {
    fetch(
        getServiceURL(url.replace(`:${searchParameter}`, encodeURIComponent(search))),
        {
            method: 'GET',
            credentials: 'include',
            headers: { Accept: 'application/json' },
            signal,
        },
    )
        .then(handleHTTPErrors)
        .then((response) => response.json())
        .then(setCompletions)
        .catch(() => {});
};

function AutoCompletion(
    {
        additionalLabel,
        color,
        input,
        url,
        onSelect,
        required,
        search,
        searchParameter,
        tooltip,
        ...props
    },
) {
    const [completions, setCompletions] = React.useState([]);
    const [isOpen, setIsOpenState] = React.useState(false);
    const [wasOpen, setWasOpen] = React.useState(false);
    const [selected, setSelected] = React.useState(0);
    const searchRef = React.useRef(null);
    const [loadCompletions] = React.useState(
        () => AwesomeDebouncePromise(loadCompletionsBounced, debouncedWaitTime),
    );

    const setIsOpen = (state) => {
        setIsOpenState(state);

        if (state) {
            setWasOpen(true);
        }
    };

    const close = () => {
        if (searchRef.current) {
            searchRef.current.blur();
        }
        setIsOpen(false);
    };

    const handleSelect = (completion) => {
        onSelect(completion);
        close();
    };

    const handleKeyDown = (event) => {
        const { key } = event;

        if (key === 'Escape') {
            close();
        } else if (key === 'Enter') {
            if (selected !== 0) {
                handleSelect(completions[selected - 1]);
                event.preventDefault();
            } else {
                close();
            }
        } else {
            if (!isOpen) {
                setIsOpen(true);
            }

            if (key === 'ArrowDown') {
                setSelected(Math.min(selected + 1, completions.length));
                event.preventDefault();
            } else if (key === 'ArrowUp') {
                setSelected(Math.max(selected - 1, 0));
                event.preventDefault();
            }
        }
    };

    React.useEffect(() => {
        if (url && wasOpen) {
            const newAbortController = new AbortController();

            loadCompletions({
                url,
                search,
                setCompletions,
                searchParameter,
                signal: newAbortController.signal,
            });

            // Cancel old request, to prevent overwriting
            return () => newAbortController.abort();
        }

        return undefined;
    }, [url, search, wasOpen]);

    React.useEffect(() => {
        setSelected(Math.min(completions.length, selected));
    }, [completions]);

    return (
        <AdvancedPopper
            additionalClassName={styles.completions}
            basic={input({
                additionalLabel,
                color: required && !search ? 'danger' : color,
                ref: searchRef,
                onKeyDown: handleKeyDown,
                tooltip,
                value: search,
            })}
            setIsOpen={setIsOpen}
            isOpen={isOpen && completions.length !== 0}
            withInput
            {...props}
        >
            <ul className={styles.entries}>
                {completions.map((completion, index) => {
                    let { id, displayName } = completion;

                    if (typeof completion === 'string') {
                        displayName = completion;
                        id = completion;
                    }

                    return (
                        <Completion
                            key={`completion-${id}`}
                            displayName={displayName}
                            onClick={() => handleSelect(completion)}
                            selected={index + 1 === selected}
                        />
                    );
                })}
            </ul>
        </AdvancedPopper>
    );
}

AutoCompletion.propTypes = {
    input: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    additionalLabel: PropTypes.string,
    color: colorPropType,
    url: PropTypes.string,
    required: PropTypes.bool,
    search: PropTypes.string,
    searchParameter: PropTypes.string,
    tooltip: PropTypes.string,
};

AutoCompletion.defaultProps = {
    additionalLabel: undefined,
    color: undefined,
    url: undefined,
    required: false,
    search: '',
    searchParameter: 'search',
    tooltip: undefined,
};

export default AutoCompletion;
