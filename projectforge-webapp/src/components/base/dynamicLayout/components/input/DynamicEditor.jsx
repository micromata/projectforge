import PropTypes from 'prop-types';
import React from 'react';
import AceEditor from 'react-ace';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/theme-github';
// import 'ace-builds/src-noconflict/theme-github';
import 'ace-builds/src-noconflict/ext-language_tools';

function DynamicEditor({ id, ...props }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    // Only rerender input when data has changed
    return React.useMemo(() => {
        const handleInputChange = (newValue) => setData({ [id]: newValue });

        return (
            <DynamicValidationManager id={id}>
                <AceEditor
                    mode="java"
                    theme="github"
                    height="800px"
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
};

DynamicEditor.defaultProps = {
};

export default DynamicEditor;
