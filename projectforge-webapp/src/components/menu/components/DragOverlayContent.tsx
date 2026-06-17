import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGripVertical, faFolder } from '@fortawesome/free-solid-svg-icons';
import { MenuItem } from '../menuCustomizerTypes';
import { isGroup } from '../menuCustomizerUtils';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
}

export function DragOverlayContent({ item }: Props) {
  return (
    <div className={styles.dragOverlayItem}>
      <FontAwesomeIcon icon={faGripVertical} style={{ color: '#c2c9d3' }} />
      {isGroup(item) && <FontAwesomeIcon icon={faFolder} style={{ color: '#2f6fed' }} />}
      <span>{item.title}</span>
    </div>
  );
}
