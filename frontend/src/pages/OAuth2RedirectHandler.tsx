import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

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
            let errorMessage = t('auth.loginError.generalError');
            if (error.includes('Account is blocked')) {
                errorMessage = t('auth.loginError.accountBlocked');
            } else if (error.includes('Account is disabled')) {
                errorMessage = t('auth.loginError.accountDisabled');
            }
            
            navigate('/login', { 
                replace: true,
                state: { error: errorMessage }
            });
        } else if (accessToken && refreshToken) {
            setTokens(accessToken, refreshToken);
            navigate('/', { replace: true });
        } else {
            navigate('/login', { 
                replace: true,
                state: { error: t('auth.loginError.generalError') }
            });
        }
    }, [navigate, searchParams, setTokens, t]);

    return null;
};

export default OAuth2RedirectHandler; 