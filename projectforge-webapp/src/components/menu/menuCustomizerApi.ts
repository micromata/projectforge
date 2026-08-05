import { baseRestURL, handleHTTPErrors } from '../../utilities/rest';
import { MenuItem, MenuCategory, Translations } from './menuCustomizerTypes';

interface PageData {
  translations: Translations;
}

interface MenuData {
  mainMenu: { menuItems: MenuCategory[] };
  favoritesMenu: { menuItems: MenuItem[] };
}

function flattenMenuItems(categories: MenuCategory[]): MenuItem[] {
  const result: MenuItem[] = [];
  for (const cat of categories) {
    if (cat.subMenu) {
      for (const item of cat.subMenu) {
        result.push(item);
      }
    }
  }
  return result;
}

export async function loadMenuData(): Promise<{
  mainMenuStructured: MenuCategory[];
  mainMenuFlat: MenuItem[];
  customMenu: MenuItem[];
  translations: Translations;
}> {
  const [pageData, menuData] = await Promise.all([
    fetch(`${baseRestURL}/menu/customizer`, {
      method: 'GET',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    }).then(handleHTTPErrors).then(r => r.json()) as Promise<PageData>,
    fetch(`${baseRestURL}/menu`, {
      method: 'GET',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    }).then(handleHTTPErrors).then(r => r.json()) as Promise<MenuData>,
  ]);

  const mainMenuStructured = menuData.mainMenu.menuItems || [];
  return {
    mainMenuStructured,
    mainMenuFlat: flattenMenuItems(mainMenuStructured),
    customMenu: menuData.favoritesMenu.menuItems || [],
    translations: pageData.translations,
  };
}

export async function saveCustomMenu(favoritesMenu: MenuItem[]): Promise<void> {
  const response = await fetch(`${baseRestURL}/menu/customized`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ favoritesMenu }),
  });
  handleHTTPErrors(response);
}

export async function loadDefaultMenu(): Promise<MenuItem[]> {
  const response = await fetch(`${baseRestURL}/menu/default`, {
    method: 'GET',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });
  handleHTTPErrors(response);
  const data = await response.json();
  return data.favoritesMenu || [];
}

export async function resetMenu(): Promise<void> {
  const response = await fetch(`${baseRestURL}/menu/reset`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({}),
  });
  handleHTTPErrors(response);
}
