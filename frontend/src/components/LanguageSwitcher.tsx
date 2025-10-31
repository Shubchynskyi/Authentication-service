import React from 'react';
import { useTranslation } from 'react-i18next';
import { Select, MenuItem, FormControl, InputLabel, SelectChangeEvent } from '@mui/material';
import { availableLanguages } from '../i18n/i18n';

const LanguageSwitcher: React.FC = () => {
  const { i18n, t } = useTranslation();
  const currentLang = (i18n.resolvedLanguage || i18n.language).split('-')[0];

  const handleLanguageChange = (event: SelectChangeEvent<string>) => {
    const language = String(event.target.value);
    try { localStorage.setItem('language', language); } catch {}
    try { document.documentElement.lang = language; } catch {}
    i18n.changeLanguage(language);
  };

  return (
    <FormControl sx={{ minWidth: 120, marginLeft: 2 }} size="small">
      <InputLabel id="language-select-label">{t('common.language')}</InputLabel>
      <Select
        labelId="language-select-label"
        id="language-select"
        value={currentLang}
        label={t('common.language')}
        onChange={handleLanguageChange}
      >
        {Object.entries(availableLanguages).map(([code, name]) => (
          <MenuItem key={code} value={code}>
            {name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};

export default LanguageSwitcher; 