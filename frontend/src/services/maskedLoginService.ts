import api from '../api';

export interface MaskedLoginPublicSettings {
    enabled: boolean;
    templateId: number;
}

export interface MaskedLoginAdminSettings extends MaskedLoginPublicSettings {
    id?: number;
    updatedAt?: string;
    updatedBy?: string;
}

export interface MaskedLoginSettingsDTO {
    enabled: boolean;
    templateId: number;
    password?: string;
}

/**
 * Gets masked login settings (public, no auth required).
 */
export const getMaskedLoginSettingsPublic = async (): Promise<MaskedLoginPublicSettings | null> => {
    try {
        const response = await api.get<MaskedLoginPublicSettings>('/api/public/masked-login/settings');
        return response.data;
    } catch (error) {
        console.error('Failed to get masked login settings:', error);
        return null;
    }
};

/**
 * Gets masked login settings for admin (includes audit fields).
 */
export const getMaskedLoginSettingsAdmin = async (): Promise<MaskedLoginAdminSettings | null> => {
    try {
        const response = await api.get<MaskedLoginAdminSettings>('/api/admin/masked-login/settings');
        return response.data;
    } catch (error) {
        console.error('Failed to get admin masked login settings:', error);
        return null;
    }
};

/**
 * Updates masked login settings.
 */
export const updateMaskedLoginSettings = async (
    settings: MaskedLoginSettingsDTO
): Promise<void> => {
    await api.put('/api/admin/masked-login/settings', settings);
};

/**
 * Gets HTML template by ID.
 */
export const getTemplate = async (templateId: number): Promise<string> => {
    const response = await api.get<string>(`/api/public/masked-login/template/${templateId}`, {
        responseType: 'text',
    });
    return response.data;
};

