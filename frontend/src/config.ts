export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
export const API_BASE_URL = `${API_URL}/api`;

// Password validation regex - must match backend pattern
// Default pattern: at least 8 characters, one digit, one lowercase, one uppercase, one special character (@#$%^&+=!-_*?), no spaces
export const PASSWORD_REGEX = new RegExp(
  import.meta.env.VITE_PASSWORD_REGEX || '^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!\\-_*?])(?=\\S+$).{8,}$'
); 