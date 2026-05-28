const API_URL = 'http://localhost:8080/api/mails';

export const mailService = {
    fetchEmails: async () => {
        const response = await fetch(`${API_URL}/fetch`);
        if (!response.ok) throw new Error('Failed to fetch emails');
        const data = await response.json();
        return data.emails;
    },

    fetchEmailDetails: async (uid) => {
        const response = await fetch(`${API_URL}/${uid}`);
        if (!response.ok){
            if (response.status === 404) throw new Error('Email not found');
            throw new Error('Failed to fetch email details');
        }
        return await response.json();
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