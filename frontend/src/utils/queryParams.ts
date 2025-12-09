export const getQueryParam = (search: string, key: string): string | null => {
  const searchParams = new URLSearchParams(search);
  const value = searchParams.get(key);
  return value;
};

