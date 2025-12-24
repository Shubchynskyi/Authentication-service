import React, { useState } from 'react';
import { Box, Typography } from '@mui/material';
import ImageIcon from '@mui/icons-material/Image';
import { useTranslation } from 'react-i18next';
import { getTemplateMetadata, type TemplateMetadata } from '../config/maskedLoginTemplates';

interface MaskedLoginTemplatePreviewProps {
  templateId: number;
}

// Import all screenshot images statically
// This ensures they work correctly in both development and production
import template01Preview from '../assets/masked-login-screenshots/template_01_preview.png';
import template02Preview from '../assets/masked-login-screenshots/template_02_preview.png';
import template03Preview from '../assets/masked-login-screenshots/template_03_preview.png';
import template04Preview from '../assets/masked-login-screenshots/template_04_preview.png';
import template05Preview from '../assets/masked-login-screenshots/template_05_preview.png';
import template06Preview from '../assets/masked-login-screenshots/template_06_preview.png';
import template07Preview from '../assets/masked-login-screenshots/template_07_preview.png';
import template08Preview from '../assets/masked-login-screenshots/template_08_preview.png';
import template09Preview from '../assets/masked-login-screenshots/template_09_preview.png';
import template10Preview from '../assets/masked-login-screenshots/template_10_preview.png';

// Map template IDs to imported images
const screenshotImages: Record<number, string> = {
  1: template01Preview,
  2: template02Preview,
  3: template03Preview,
  4: template04Preview,
  5: template05Preview,
  6: template06Preview,
  7: template07Preview,
  8: template08Preview,
  9: template09Preview,
  10: template10Preview,
};

/**
 * Component that displays a preview screenshot of a masked login template
 * with visual indicators showing where to click for login.
 */
const MaskedLoginTemplatePreview: React.FC<MaskedLoginTemplatePreviewProps> = ({ templateId }) => {
  const { t } = useTranslation();
  const [imageError, setImageError] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  const metadata: TemplateMetadata | undefined = getTemplateMetadata(templateId);

  if (!metadata) {
    return null;
  }

  const handleImageError = () => {
    setImageError(true);
    setImageLoaded(false);
  };

  const handleImageLoad = () => {
    setImageLoaded(true);
    setImageError(false);
  };

  // Get image URL directly from the static imports
  // This works reliably in both development and production
  const imageSrc = screenshotImages[templateId];

  // Show placeholder if imageSrc is undefined, null, or empty, or if there was an error loading
  const showPlaceholder = imageError || !imageSrc;

  return (
    <Box
      sx={{
        position: 'relative',
        width: '100%',
        maxWidth: 800,
        minHeight: 400,
        margin: '20px auto',
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        overflow: 'hidden',
        backgroundColor: 'background.paper'
      }}
    >
      {showPlaceholder ? (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 400,
            padding: 4,
            backgroundColor: 'grey.100',
            border: '2px dashed',
            borderColor: 'grey.400',
            borderRadius: 2
          }}
        >
          <ImageIcon sx={{ fontSize: 64, color: 'grey.400', mb: 2 }} />
          <Typography variant="body1" color="text.secondary">
            {t('admin.maskedLogin.screenshotPlaceholder')}
          </Typography>
        </Box>
      ) : (
        <Box sx={{ position: 'relative', width: '100%' }}>
          <img
            src={imageSrc}
            alt={`${metadata.name} preview`}
            onError={handleImageError}
            onLoad={handleImageLoad}
            style={{
              width: '100%',
              height: 'auto',
              display: imageLoaded ? 'block' : 'none'
            }}
          />
        </Box>
      )}
      {/* Description text below the preview */}
      <Box sx={{ padding: 2, backgroundColor: 'background.default', borderTop: '1px solid', borderColor: 'divider' }}>
        <Typography variant="caption" color="text.secondary">
          <strong>{t('admin.maskedLogin.template')} {templateId}:</strong> {metadata.name}
        </Typography>
        {!showPlaceholder && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {metadata.clickArea.description}
          </Typography>
        )}
      </Box>
    </Box>
  );
};

export default MaskedLoginTemplatePreview;

