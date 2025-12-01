import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import { clearTokens } from '../utils/token';

const OAuth2RedirectHandler = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { setTokens } = useAuth();
    const { t } = useTranslation();

    useEffect(() => {
        const accessToken = searchParams.get('accessToken');
        const refreshToken = searchParams.get('refreshToken');
        const error = searchParams.get('error');
        
        if (error) {
            // Clear any existing tokens on error
            clearTokens();
            
            let errorMessage = t('auth.loginError.generalError');
            if (error.includes('Account is blocked')) {
                const blockReason = error.split('Account is blocked.')[1]?.trim();
                errorMessage = t('auth.loginError.accountBlocked') + 
                    (blockReason ? `: ${blockReason}` : '');
            } else if (error.includes('Account is disabled')) {
                errorMessage = t('auth.loginError.accountDisabled');
            } else if (error.includes('not in whitelist')) {
                errorMessage = t('auth.loginError.notInWhitelist') || error;
            }
            
            navigate('/', { 
                replace: true,
                state: { error: errorMessage }
            });
        } else if (accessToken && refreshToken) {
            // Clear old tokens before setting new ones
            clearTokens();
            setTokens(accessToken, refreshToken);
            navigate('/', { replace: true });
        } else {
            // Clear tokens if no valid tokens received
            clearTokens();
            navigate('/', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
        }
    }, [navigate, searchParams, setTokens, t]);

    return null;
};

export default OAuth2RedirectHandler; 