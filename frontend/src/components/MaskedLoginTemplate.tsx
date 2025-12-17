import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

interface MaskedLoginTemplateProps {
    htmlContent: string;
}

/**
 * Component that renders HTML template and handles clicks on hidden elements.
 * Hidden elements with links to /login?secret=true or /register?secret=true
 * will trigger navigation to the actual login/register pages.
 */
const MaskedLoginTemplate: React.FC<MaskedLoginTemplateProps> = ({ htmlContent }) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();

    useEffect(() => {
        if (!containerRef.current) return;

        // Handle clicks on links that contain secret=true parameter
        const handleClick = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            const link = target.closest('a');
            
            if (link) {
                const href = link.getAttribute('href');
                if (href && href.includes('secret=true')) {
                    e.preventDefault();
                    // Extract the path (e.g., /login or /register)
                    const url = new URL(href, window.location.origin);
                    navigate(url.pathname + url.search);
                }
            }
        };

        const container = containerRef.current;
        container.addEventListener('click', handleClick);

        return () => {
            container.removeEventListener('click', handleClick);
        };
    }, [navigate]);

    return (
        <div
            ref={containerRef}
            dangerouslySetInnerHTML={{ __html: htmlContent }}
            style={{ width: '100%', height: '100vh', overflow: 'auto' }}
        />
    );
};

export default MaskedLoginTemplate;

