import React from 'react';
import { useTranslation } from 'react-i18next';
import { Select, MenuItem, FormControl, InputLabel, SelectChangeEvent } from '@mui/material';
import { availableLanguages } from '../i18n/i18n';

type LanguageSwitcherVariant = 'default' | 'compact';

interface LanguageSwitcherProps {
  variant?: LanguageSwitcherVariant;
}

const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({ variant = 'default' }) => {
  const { i18n, t } = useTranslation();
  const currentLang = (i18n.resolvedLanguage || i18n.language).split('-')[0];

  const handleLanguageChange = (event: SelectChangeEvent<string>) => {
    const language = String(event.target.value);
    try { localStorage.setItem('language', language); } catch {}
    try { document.documentElement.lang = language; } catch {}
    i18n.changeLanguage(language);
  };

  const isCompact = variant === 'compact';
  const controlWidth = isCompact ? 90 : 120;
  const labelId = isCompact ? undefined : 'language-select-label';

  return (
    <FormControl sx={{ minWidth: controlWidth, marginLeft: 2 }} size="small">
      {!isCompact && (
        <InputLabel id={labelId}>{t('common.language')}</InputLabel>
      )}
      <Select
        labelId={labelId}
        id="language-select"
        value={currentLang}
        label={isCompact ? undefined : t('common.language')}
        onChange={handleLanguageChange}
        displayEmpty={isCompact}
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