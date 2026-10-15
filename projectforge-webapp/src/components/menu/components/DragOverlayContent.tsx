import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolder } from '@fortawesome/free-solid-svg-icons';
import { MenuItem } from '../menuCustomizerTypes';
import { isGroup } from '../menuCustomizerUtils';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
}

export function DragOverlayContent({ item }: Props) {
  if (isGroup(item)) {
    return (
      <div className={styles.dragOverlayItem}>
        <FontAwesomeIcon icon={faFolder} style={{ color: '#2f6fed' }} />
        <span>{item.title}</span>
      </div>
    );
  }

  return (
    <div className={styles.dragOverlayItem}>
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#2f6fed', flexShrink: 0 }} />
      <span>{item.title}</span>
    </div>
  );
}
