import React from 'react';

export const useConstant = (calculateInitialValue, inputs) => {
    const [constant, setConstant] = React.useState(calculateInitialValue());

    React.useEffect(() => {
        setConstant(calculateInitialValue);
    }, inputs);

    return constant;
};

export const useMouseUpkHandler = (handler, inputs, active = true) => {
    React.useEffect(() => {
        if (active) {
            document.addEventListener('mouseup', handler);

            return () => document.removeEventListener('mouseup', handler);
        }

        return undefined;
    }, inputs);
};

export const useClickOutsideHandler = (reference, callback, active) => useMouseUpkHandler(
    ({ target }) => {
        if (reference.current && !reference.current.contains(target)) {
            callback(false);
        }
    },
    [active],
    active,
);
