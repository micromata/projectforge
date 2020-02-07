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
        .then(response => response.json())
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
        ...props
    },
) {
    const [completions, setCompletions] = React.useState([]);
    const [isOpen, setIsOpen] = React.useState(false);
    const [abortController, setAbortController] = React.useState(null);
    const searchRef = React.useRef(null);
    const [loadCompletions] = React.useState(
        () => AwesomeDebouncePromise(loadCompletionsBounced, debouncedWaitTime),
    );

    const close = () => {
        if (searchRef.current) {
            searchRef.current.blur();
        }
        setIsOpen(false);
    };

    const handleKeyDown = ({ key }) => {
        if (key === 'Escape' || key === 'Enter') {
            close();
        } else if (!isOpen) {
            setIsOpen(true);
        }
    };

    const handleSelect = (completion) => {
        onSelect(completion);
        close();
    };

    React.useEffect(() => {
        if (url) {
            // Cancel old request, to prevent overwriting
            if (abortController) {
                abortController.abort();
            }

            const newAbortController = new AbortController();
            setAbortController(newAbortController);

            loadCompletions({
                url,
                search,
                setCompletions,
                searchParameter,
                signal: newAbortController.signal,
            });
        }
    }, [url, search]);

    return (
        <AdvancedPopper
            additionalClassName={styles.completions}
            basic={input({
                additionalLabel,
                color: required && !search ? 'danger' : color,
                ref: searchRef,
                onKeyDown: handleKeyDown,
                value: search,
            })}
            setIsOpen={setIsOpen}
            isOpen={isOpen && completions.length !== 0}
            withInput
            {...props}
        >
            <ul className={styles.entries}>
                {completions.map((completion) => {
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
};

AutoCompletion.defaultProps = {
    additionalLabel: undefined,
    color: undefined,
    url: undefined,
    required: false,
    search: '',
    searchParameter: 'search',
};

export default AutoCompletion;
