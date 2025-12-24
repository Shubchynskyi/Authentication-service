import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLayout } from '../context/LayoutContext';

interface MaskedLoginTemplateProps {
    htmlContent: string;
}

/**
 * Component that renders HTML template and handles clicks on hidden elements.
 * Hidden elements with links to /login?secret=true or /register?secret=true
 * will trigger navigation to the actual login/register pages.
 * This component also hides the Navbar/Toolbar when displayed.
 */
const MaskedLoginTemplate: React.FC<MaskedLoginTemplateProps> = ({ htmlContent }) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();
    const { setHideNavbar } = useLayout();

    // Hide navbar when template is mounted
    useEffect(() => {
        setHideNavbar(true);
        return () => {
            setHideNavbar(false);
        };
    }, [setHideNavbar]);

    useEffect(() => {
        if (!containerRef.current) return;

        const container = containerRef.current;

        // Extract styles from <head> and body content
        const parser = new DOMParser();
        const doc = parser.parseFromString(htmlContent, 'text/html');
        const styleElement = doc.querySelector('head style');
        const bodyElement = doc.querySelector('body');
        
        if (styleElement && bodyElement) {
            const styleContent = styleElement.textContent || '';
            const bodyContent = bodyElement.innerHTML;
            
            // Update container innerHTML with body content
            container.innerHTML = bodyContent;
            
            // Inject styles into document head, replacing body/html selectors with container selector
            const styleId = 'masked-login-template-styles';
            let existingStyle = document.getElementById(styleId);
            if (existingStyle) {
                existingStyle.remove();
            }
            
            // Modify styles to target the container and its children
            const containerId = 'masked-login-container';
            let scopedStyles = styleContent
                .replace(/html\s*,\s*body\s*\{/g, `#${containerId} {`)
                .replace(/body\s*\{/g, `#${containerId} {`);
            
            // Extract body styles and apply them directly to container
            const bodyStyleMatch = styleContent.match(/body\s*\{([^}]*)\}/);
            if (bodyStyleMatch) {
                const bodyStyles = bodyStyleMatch[1].trim();
                // Apply body styles to container via inline styles
                const stylePairs = bodyStyles.split(';').filter(s => s.trim());
                stylePairs.forEach(pair => {
                    const [property, value] = pair.split(':').map(s => s.trim());
                    if (property && value) {
                        const camelProperty = property.replace(/-([a-z])/g, (g) => g[1].toUpperCase());
                        // Don't override position, width, height, zIndex as they're set inline
                        if (!['position', 'width', 'height', 'zIndex', 'top', 'left'].includes(camelProperty)) {
                            (container as any).style[camelProperty] = value;
                        }
                    }
                });
            }
            
            const newStyle = document.createElement('style');
            newStyle.id = styleId;
            newStyle.textContent = scopedStyles;
            document.head.appendChild(newStyle);
        } else {
            // Fallback: if no full HTML structure, use content as-is
            container.innerHTML = htmlContent;
        }

        // Execute scripts in the template HTML
        const scripts = container.querySelectorAll('script');
        scripts.forEach((oldScript) => {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach((attr) => {
                newScript.setAttribute(attr.name, attr.value);
            });
            newScript.textContent = oldScript.textContent;
            oldScript.parentNode?.replaceChild(newScript, oldScript);
        });

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

        container.addEventListener('click', handleClick);

        return () => {
            container.removeEventListener('click', handleClick);
            // Clean up injected styles
            const styleElement = document.getElementById('masked-login-template-styles');
            if (styleElement) {
                styleElement.remove();
            }
        };
    }, [navigate, htmlContent]);

    // Hide body scroll and ensure template takes full screen
    useEffect(() => {
        document.body.style.overflow = 'hidden';
        return () => {
            document.body.style.overflow = '';
        };
    }, []);

    return (
        <div
            id="masked-login-container"
            ref={containerRef}
            style={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100vw',
                height: '100vh',
                overflow: 'auto',
                zIndex: 10000,
                margin: 0,
                padding: 0
            }}
        />
    );
};

export default MaskedLoginTemplate;

