import React from 'react';

/*
* Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/FileHeader.tsx
*/
export interface FileHeaderProps {
    file: File;
}

export function FileHeader({ file }: FileHeaderProps) {
    return <div>{file.name}</div>;
}
