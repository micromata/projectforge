import React, { useEffect, useState } from 'react';
import { Progress } from 'reactstrap';
import { FileHeader } from './FileHeader';

/*
 * Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/SingleFileUploadWithProgress.tsx
 */

/* eslint-disable max-len */

export interface SingleFileUploadWithProgressProps {
    file: File;
    url: string;
    onUpload: (file: File, url: string) => void;
    afterFileUpload: (file: File, response: string) => void;
}

function uploadFile(
    file: File,
    url: string,
    onProgress: (percentage: number) => void,
    afterFileUpload: (afterFile: File, response: string) => void,
) {
    const key = 'docs_upload_example_us_preset';

    return new Promise<string>((res, rej) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', url);
        xhr.withCredentials = true;

        xhr.onload = () => {
            const resp = JSON.parse(xhr.responseText);
            res(resp.secure_url);
        };
        xhr.onerror = (evt) => rej(evt);
        xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
                const percentage = (event.loaded / event.total) * 100;
                onProgress(Math.round(percentage));
            }
        };
        xhr.onreadystatechange = () => {
            if (xhr.readyState === 4) {
                afterFileUpload(file, xhr.responseText);
            }
        };
        const formData = new FormData();
        formData.append('file', file);
        formData.append('upload_preset', key);

        xhr.send(formData);
    });
}

export function SingleFileUploadWithProgress({
    file,
    url,
    onUpload,
    afterFileUpload,
}: SingleFileUploadWithProgressProps) {
    const [progress, setProgress] = useState(0);

    let color = 'warning';
    let animated = true;
    if (progress === 100) {
        color = 'success';
        animated = false;
    }

    useEffect(() => {
        async function upload() {
            await uploadFile(file, url, setProgress, afterFileUpload);
            onUpload(file, url);
        }

        upload();
    }, []);

    return (
        <div className="uploadProgress">
            <FileHeader file={file} />
            <Progress
                animated={animated}
                color={color}
                value={progress}
            >
                {progress}
                {' %'}
            </Progress>
        </div>
    );
}
