import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const OAuth2RedirectHandler = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { setTokens } = useAuth();

    useEffect(() => {
        const accessToken = searchParams.get('accessToken');
        const refreshToken = searchParams.get('refreshToken');
        
        if (accessToken && refreshToken) {
            setTokens(accessToken, refreshToken);
            navigate('/', { replace: true });
        } else {
            navigate('/login', { 
                replace: true,
                state: { 
                    error: 'Error logging in with Google. Please try again.' 
                } 
            });
        }
    }, [navigate, searchParams, setTokens]);

    return null;
};

export default OAuth2RedirectHandler; 