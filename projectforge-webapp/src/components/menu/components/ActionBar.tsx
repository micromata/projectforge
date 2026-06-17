import React from 'react';
import { Button } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSave, faUndo } from '@fortawesome/free-solid-svg-icons';
import { Translations } from '../menuCustomizerTypes';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  translations: Translations;
  isDirty: boolean;
  onSave: () => void;
  onUndo: () => void;
  onLoadDefault: () => void;
  onReset: () => void;
}

export function ActionBar({ translations, isDirty, onSave, onUndo, onLoadDefault, onReset }: Props) {
  return (
    <div className={styles.actionButtons}>
      <Button color="primary" onClick={onSave} disabled={!isDirty}>
        <FontAwesomeIcon icon={faSave} />{' '}
        <span>{translations.saveChanges || 'Save Changes'}</span>
      </Button>
      <Button color="secondary" onClick={onUndo} disabled={!isDirty}>
        <FontAwesomeIcon icon={faUndo} />{' '}
        <span>{translations.undo || 'Undo'}</span>
      </Button>
      <Button color="warning" onClick={onLoadDefault}>
        <FontAwesomeIcon icon={faUndo} />{' '}
        <span>{translations.loadDefault || 'Load Default'}</span>
      </Button>
      <Button color="danger" onClick={onReset}>
        <span>{translations.resetToDefault || 'Reset to Default'}</span>
      </Button>
    </div>
  );
}
