import PropTypes from 'prop-types';
import React from 'react';
import AceEditor from 'react-ace';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

import 'ace-builds/src-noconflict/mode-kotlin';
import 'ace-builds/src-noconflict/mode-groovy';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/ext-searchbox';
import 'ace-builds/src-noconflict/ext-language_tools';

function DynamicEditor({ id, mode, ...props }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    // Only rerender input when data has changed
    return React.useMemo(() => {
        const handleInputChange = (newValue) => setData({ [id]: newValue });

        return (
            <DynamicValidationManager id={id}>
                <AceEditor
                    mode={mode || 'kotlin'}
                    theme="monokai"
                    height="600px"
                    width="1024px"
                    value={value}
                    onChange={handleInputChange}
                    name={`${ui.uid}-${id}`}
                    setOptions={{
                        enableBasicAutocompletion: true,
                        enableLiveAutocompletion: true,
                        enableSnippets: true,
                        showLineNumbers: true,
                        tabSize: 2,
                    }}
                    enableLiveAutocompletion="true"
                    editorProps={{ $blockScrolling: true }}
                />
            </DynamicValidationManager>
        );
    }, [value, setData, id, props]);
}

DynamicEditor.propTypes = {
    id: PropTypes.string.isRequired,
    mode: PropTypes.string,
};

DynamicEditor.defaultProps = {
};

export default DynamicEditor;
