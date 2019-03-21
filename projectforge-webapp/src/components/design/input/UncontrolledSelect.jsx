import React from 'react';
import { uncontrolledSelectProps } from '../../../utilities/propTypes';
import Select from './Select';

// A Wrapper around Select to automatically handle changes.
// You cannot access the current value with this Component!
function UncontrolledSelect(props) {
    const { options } = props;
    const [selected, setSelected] = React.useState(options[0].value);

    return (
        <Select selected={selected} setSelected={setSelected} {...props} />
    );
}

UncontrolledSelect.propTypes = {
    ...uncontrolledSelectProps,
};

export default UncontrolledSelect;
