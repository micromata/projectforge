import React from 'react';
import MagicColorPicker from './MagicColorPicker';
import MagicInputNotImplemented from './MagicInputNotImplemented';
import MagicSelectInput from './MagicSelectInput';
import MagicStringInput from './MagicStringInput';

const useMagicInput = (type) => {
    const [inputTag, setInputTag] = React.useState(() => MagicInputNotImplemented);

    React.useEffect(() => {
        let tag;

        switch (type) {
            case 'STRING':
                tag = MagicStringInput;
                break;
            case 'COLOR_PICKER':
                tag = MagicColorPicker;
                break;
            case 'SELECT':
                tag = MagicSelectInput;
                break;
            default:
                tag = MagicInputNotImplemented;
                break;
        }

        if (tag !== inputTag) {
            // Important to use a state function to prevent the tag to be called
            setInputTag(() => tag);
        }
    }, [type]);

    return inputTag;
};

export default useMagicInput;
