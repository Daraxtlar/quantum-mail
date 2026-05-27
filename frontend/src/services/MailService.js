const API_URL = 'http://localhost:8080/api/mails';

export const mailService = {
    fetchEmails: async () => {
        const response = await fetch(`${API_URL}/fetch`);
        if (!response.ok) throw new Error('Failed to fetch emails');
        const data = await response.json();
        return data.emails;
    },

    sendEmail: async (formData) => {
        const response = await fetch(`${API_URL}/send`, {
            method: 'POST',
            body: formData,
        });
        if (!response.ok) throw new Error('Failed to send email');
        return response.json();
    },

};