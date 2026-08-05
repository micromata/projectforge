import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck } from '@fortawesome/free-solid-svg-icons';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  message: string;
}

export function Toast({ message }: Props) {
  return (
    <div className={styles.toast}>
      <FontAwesomeIcon icon={faCheck} className={styles.toastCheck} />
      <span>{message}</span>
    </div>
  );
}
