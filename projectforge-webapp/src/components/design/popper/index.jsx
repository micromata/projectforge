import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { usePopper } from 'react-popper';
import style from './Popper.module.scss';

function Popper(
    {
        className,
        children,
        direction,
        isOpen,
        target,
        ...props
    },
) {
    const [targetRef, setTargetRef] = useState(null);
    const [popperElement, setPopperElement] = useState(null);
    const [arrowElement, setArrowElement] = useState(null);
    const { styles, attributes } = usePopper(targetRef, popperElement, {
        placement: direction,
        modifiers: [{ name: 'arrow', options: { element: arrowElement } }],
    });

    return (
        <>
            <div ref={setTargetRef}>{target}</div>
            <div
                ref={setPopperElement}
                style={styles.popper}
                {...attributes.popper}
                className={classNames(style.popper, className)}
                {...props}
            >
                {children}
                <div ref={setArrowElement} style={styles.arrow} />
            </div>
        </>
    );
}

Popper.propTypes = {
    children: PropTypes.node.isRequired,
    target: PropTypes.node.isRequired,
    className: PropTypes.string,
    direction: PropTypes.string,
    isOpen: PropTypes.bool,
};

Popper.defaultProps = {
    className: undefined,
    direction: 'auto',
    isOpen: false,
};

export default Popper;
