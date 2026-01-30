import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import { clearTokens, isValidJwtFormat } from '../utils/token';

const OAuth2RedirectHandler = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { setTokens } = useAuth();
    const { t } = useTranslation();

    useEffect(() => {
        // Error comes via query parameters (?error=...)
        const error = searchParams.get('error');
        
        if (error) {
            // Clear any existing tokens on error
            clearTokens();
            
            // Decode error message if it was URL-encoded
            let decodedError: string;
            try {
                decodedError = decodeURIComponent(error);
            } catch {
                decodedError = error;
            }
            
            // Handle invalid_credentials error - redirect to login page with standard error message
            if (decodedError === 'invalid_credentials') {
                navigate('/login?secret=true', {
                    replace: true,
                    state: { error: t('errors.loginFailed') }
                });
                return;
            }

            let errorMessage = t('auth.loginError.generalError');
            if (decodedError.includes('Account is blocked')) {
                const blockReason = decodedError.split('Account is blocked.')[1]?.trim();
                errorMessage = t('auth.loginError.accountBlocked') + 
                    (blockReason ? `: ${blockReason}` : '');
            } else if (decodedError.includes('Account is disabled')) {
                errorMessage = t('auth.loginError.accountDisabled');
            } else if (decodedError.includes('not in whitelist')) {
                errorMessage = t('auth.loginError.notInWhitelist') || decodedError;
            }
            
            navigate('/', { 
                replace: true,
                state: { error: errorMessage }
            });
            return;
        }

        // Token comes via URL fragment (#accessToken=...)
        // URL fragment is not sent to server, providing better security
        const hash = window.location.hash;
        if (!hash || hash.length <= 1) {
            // No hash fragment, clear tokens and redirect with error
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
            return;
        }

        // Parse hash fragment (remove leading #)
        const hashParams = new URLSearchParams(hash.substring(1));
        const encodedAccessToken = hashParams.get('accessToken');
        
        if (!encodedAccessToken) {
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
            return;
        }

        // Decode URL-encoded token
        let accessToken: string;
        try {
            accessToken = decodeURIComponent(encodedAccessToken);
        } catch (decodeError) {
            console.error('Failed to decode tokens from URL:', decodeError);
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
            return;
        }

        // Validate JWT token format before saving
        if (!isValidJwtFormat(accessToken)) {
            console.error('Invalid JWT token format received');
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
            return;
        }

        // Clear hash from URL for security (tokens should not remain in browser history)
        window.history.replaceState(null, '', window.location.pathname);

        // Set new tokens
        setTokens(accessToken);
        navigate('/', { replace: true });
    }, [navigate, searchParams, setTokens, t]);

    return null;
};

export default OAuth2RedirectHandler;
