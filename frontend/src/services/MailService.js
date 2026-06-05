const API_URL = 'http://localhost:8080/api/mails';

export const mailService = {
    fetchEmails: async (folder = 'INBOX', page = 0, size = 20) => {
        const response = await fetch(`${API_URL}/fetch?folder=${folder}&page=${page}&size=${size}`);
        if (!response.ok) throw new Error('Failed to fetch emails');
        return await response.json();
    },

    fetchEmailDetails: async (folder, uid) => {
        const response = await fetch(`${API_URL}/${folder}/${uid}`);
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