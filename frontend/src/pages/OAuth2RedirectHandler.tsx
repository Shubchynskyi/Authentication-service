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

        // Get and decode tokens from URL parameters
        const encodedAccessToken = searchParams.get('accessToken');
        const encodedRefreshToken = searchParams.get('refreshToken');
        
        if (!encodedAccessToken || !encodedRefreshToken) {
            // Clear tokens if no valid tokens received
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
            return;
        }

        // Decode URL-encoded tokens
        let accessToken: string;
        let refreshToken: string;
        try {
            accessToken = decodeURIComponent(encodedAccessToken);
            refreshToken = decodeURIComponent(encodedRefreshToken);
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
        if (!isValidJwtFormat(accessToken) || !isValidJwtFormat(refreshToken)) {
            console.error('Invalid JWT token format received');
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
            return;
        }

        // Set new tokens
        setTokens(accessToken, refreshToken);
        navigate('/', { replace: true });
    }, [navigate, searchParams, setTokens, t]);

    return null;
};

export default OAuth2RedirectHandler; 