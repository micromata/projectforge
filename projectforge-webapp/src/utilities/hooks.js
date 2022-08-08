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
            const options = {};
            document.addEventListener('click', handler, options);
            return () => document.removeEventListener('click', handler, options);
        }

        return undefined;
    }, inputs);
};

export const useClickOutsideHandler = (reference, callback, active) => useMouseClickHandler(
    (event) => {
        const { target } = event;
        if (reference.current && !reference.current.contains(target)) {
            callback(false, event);
        }
    },
    [active],
    active,
);
