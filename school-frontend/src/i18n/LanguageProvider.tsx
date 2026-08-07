import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { translations, type Language } from './translations';

const LANGUAGE_KEY = 'sms-language';

function loadLanguage(): Language {
  return localStorage.getItem(LANGUAGE_KEY) === 'hi' ? 'hi' : 'en';
}

interface LanguageContextValue {
  language: Language;
  setLanguage: (lang: Language) => void;
}

const LanguageContext = createContext<LanguageContextValue | undefined>(undefined);

/** Persists the selected UI language to localStorage and exposes it via useLanguage()/useTranslation(). */
export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>(loadLanguage);

  const value = useMemo<LanguageContextValue>(
    () => ({
      language,
      setLanguage: (lang: Language) => {
        setLanguageState(lang);
        localStorage.setItem(LANGUAGE_KEY, lang);
      },
    }),
    [language],
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage(): LanguageContextValue {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useLanguage must be used within a LanguageProvider');
  return ctx;
}

type Dict = Record<string, Record<string, string>>;

function lookup(dict: Dict, key: string): string | undefined {
  const [group, leaf] = key.split('.');
  return leaf ? dict[group]?.[leaf] : undefined;
}

/**
 * Dot-path translation lookup: `t('nav.dashboard')`. Falls back to the
 * English string, then to the raw key itself, so a missing/incomplete
 * translation never crashes or renders blank — this keeps the mechanism safe
 * to extend incrementally as more of the app is migrated.
 */
export function useTranslation() {
  const { language } = useLanguage();
  return function t(key: string): string {
    return lookup(translations[language] as Dict, key) ?? lookup(translations.en as Dict, key) ?? key;
  };
}
