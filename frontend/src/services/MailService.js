const API_URL = 'http://localhost:8080/api/mails';

const getHeaders = () => {
    const token = localStorage.getItem('token');
    return{
        'Content-type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };
};

const handleResponse = async (response) => {
    if (response.status === 401) {
        localStorage.removeItem('token');
        window.location.href = '/login';
        throw new Error("SESSION_EXPIRED");
    }
    if (!response.ok) throw new Error("Błąd serwera");
    return response;
};

export const mailService = {
    fetchEmails: async (folder = 'INBOX', page = 0, size = 20) => {
        const response = await fetch(`${API_URL}/fetch?folder=${folder}&page=${page}&size=${size}`, {
            headers: getHeaders()
        });
        await handleResponse(response);
        return await response.json();
    },

    fetchEmailDetails: async (folder, uid) => {
        const response = await fetch(`${API_URL}/${folder}/${uid}`, {
            headers: getHeaders()
        });
        await handleResponse(response);
        return await response.json();
    },

    sendEmail: async (formData) => {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_URL}/send`, {
            method: 'POST',
            headers: {'Authorization': `Bearer ${token}`},
            body: formData,
        });
        await handleResponse(response);
        return response.json();
    },

    fetchSuggestions: async (senderEmail) => {
        const response = await fetch(`${API_URL}/suggestions?senderEmail=${encodeURIComponent(senderEmail)}`, {
            headers: getHeaders()
        });
        await handleResponse(response);
        return await response.json();
    },

};