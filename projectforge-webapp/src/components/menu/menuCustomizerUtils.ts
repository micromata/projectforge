import { MenuItem } from './menuCustomizerTypes';

export function getItemId(item: MenuItem): string {
  return item.id || item.key || '';
}

export function isGroup(item: MenuItem): boolean {
  return Array.isArray(item.subMenu);
}

export function isDuplicate(customMenu: MenuItem[], itemId: string): boolean {
  for (const item of customMenu) {
    if (getItemId(item) === itemId) return true;
    if (item.subMenu) {
      for (const sub of item.subMenu) {
        if (getItemId(sub) === itemId) return true;
      }
    }
  }
  return false;
}

export function cleanMenuForSave(menu: MenuItem[]): MenuItem[] {
  return menu.map(item => {
    if (item.subMenu && item.subMenu.length > 0) {
      return {
        ...item,
        subMenu: item.subMenu.map(({ subMenu: _sub, ...rest }) => rest),
      };
    }
    const { subMenu: _sub, ...cleanItem } = item;
    return cleanItem;
  });
}
