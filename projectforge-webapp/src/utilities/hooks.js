import React from 'react';

export const useConstant = (calculateInitialValue, inputs) => {
    const [constant, setConstant] = React.useState(calculateInitialValue());

    React.useEffect(() => {
        setConstant(calculateInitialValue);
    }, inputs);

    return constant;
};

export const useMouseClickHandler = (handler, inputs, active = true) => {
    React.useEffect(() => {
        if (active) {
            console.log('add', active);
            document.addEventListener('click', handler, { capture: true });
            return () => {
                console.log('remove');
                document.removeEventListener('click', handler);
            };
        }

        return undefined;
    }, inputs);
};

export const useClickOutsideHandler = (reference, callback, active) => useMouseClickHandler(
    (event) => {
        const { target } = event;
        if (reference.current && !reference.current.contains(target)) {
            event.preventDefault();
            event.stopPropagation();
            callback(false, event);
            return false;
        }
        return undefined;
    },
    [active],
    active,
);
