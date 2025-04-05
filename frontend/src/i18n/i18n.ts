import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import Backend from 'i18next-http-backend';

import ru from './locales/ru.json';
import en from './locales/en.json';
import uk from './locales/uk.json';
import de from './locales/de.json';

export const availableLanguages = {
  ru: 'Русский',
  en: 'English',
  uk: 'Українська',
  de: 'Deutsch'
};

// Определяем язык браузера
const getBrowserLanguage = () => {
  const storedLanguage = localStorage.getItem('language');
  if (storedLanguage && Object.keys(availableLanguages).includes(storedLanguage)) {
    return storedLanguage;
  }
  
  const browserLang = navigator.language.split('-')[0];
  return Object.keys(availableLanguages).includes(browserLang) ? browserLang : 'en';
};

i18n
  .use(Backend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      ru: { translation: ru },
      en: { translation: en },
      uk: { translation: uk },
      de: { translation: de }
    },
    lng: getBrowserLanguage(),
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    },
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'language',
      caches: ['localStorage'],
    },
  });

export default i18n; 