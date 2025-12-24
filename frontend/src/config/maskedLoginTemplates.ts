/**
 * Configuration for masked login templates.
 * Contains metadata about templates including screenshot paths and click area information.
 */

export interface TemplateClickArea {
  // CSS selector or description of the clickable element
  selector?: string;
  // Description of where to click (for visual annotations)
  description: string;
  // Optional: relative position hints (top, left, right, bottom) for visual indicators
  positionHint?: {
    top?: string;
    left?: string;
    right?: string;
    bottom?: string;
  };
}

export interface TemplateMetadata {
  id: number;
  name: string;
  screenshotPath: string; // Path will be resolved when images are added
  clickArea: TemplateClickArea;
}

/**
 * Template metadata mapping.
 * Screenshots should be placed in: frontend/src/assets/masked-login-screenshots/
 * Format: template_XX_preview.png (where XX is 01-10)
 */
export const MASKED_LOGIN_TEMPLATES: Record<number, TemplateMetadata> = {
  1: {
    id: 1,
    name: '404 Not Found',
    screenshotPath: 'template_01_preview.png',
    clickArea: {
      description: 'Click on the dot after "nginx/1.18"',
      positionHint: { bottom: '10%', right: '15%' }
    }
  },
  2: {
    id: 2,
    name: 'Site Maintenance',
    screenshotPath: 'template_02_preview.png',
    clickArea: {
      description: 'Click on the dot after "shortly"',
      positionHint: { bottom: '20%', right: '10%' }
    }
  },
  3: {
    id: 3,
    name: 'Cooking Recipe',
    screenshotPath: 'template_03_preview.png',
    clickArea: {
      description: 'Click on the dot after "apples" in the instructions',
      positionHint: { bottom: '25%', left: '30%' }
    }
  },
  4: {
    id: 4,
    name: 'Terms of Service',
    screenshotPath: 'template_04_preview.png',
    clickArea: {
      description: 'Click on the dot after "Terms of Service" in the footer',
      positionHint: { bottom: '15%', left: '20%' }
    }
  },
  5: {
    id: 5,
    name: 'About Us',
    screenshotPath: 'template_05_preview.png',
    clickArea: {
      description: 'Click on the dot after "trustworthiness" in the highlighted section',
      positionHint: { bottom: '50%', left: '50%' }
    }
  },
  6: {
    id: 6,
    name: 'Cat Facts',
    screenshotPath: 'template_06_preview.png',
    clickArea: {
      description: 'Click on the dot after "naps"',
      positionHint: { bottom: '30%', left: '25%' }
    }
  },
  7: {
    id: 7,
    name: 'Lorem Ipsum',
    screenshotPath: 'template_07_preview.png',
    clickArea: {
      description: 'Click on the dot after "explicabo"',
      positionHint: { bottom: '25%', left: '30%' }
    }
  },
  8: {
    id: 8,
    name: 'Weather',
    screenshotPath: 'template_08_preview.png',
    clickArea: {
      description: 'Click on the dot after "Cloudy"',
      positionHint: { bottom: '20%', right: '20%' }
    }
  },
  9: {
    id: 9,
    name: 'Coming Soon',
    screenshotPath: 'template_09_preview.png',
    clickArea: {
      description: 'Click on the dot after "Alpha" in the footer',
      positionHint: { bottom: '10%', left: '25%' }
    }
  },
  10: {
    id: 10,
    name: 'Database Error',
    screenshotPath: 'template_10_preview.png',
    clickArea: {
      description: 'Click on the dot after "host"',
      positionHint: { bottom: '15%', left: '20%' }
    }
  }
};

/**
 * Get template metadata by ID
 */
export function getTemplateMetadata(templateId: number): TemplateMetadata | undefined {
  return MASKED_LOGIN_TEMPLATES[templateId];
}

/**
 * Get screenshot path for template
 */
export function getTemplateScreenshotPath(templateId: number): string | undefined {
  const metadata = getTemplateMetadata(templateId);
  return metadata?.screenshotPath;
}

