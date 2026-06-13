const API_URL = "http://localhost:8080/api/email_accounts";

const getHeaders = () => {
    const token = localStorage.getItem("token");
    return {
        "Authorization": `Bearer ${token}`,
        "Content-type": "application/json"
    };
};

const handleResponse = async (response) => {
    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('token');
        window.location.href = '/login';
        throw new Error("SESSION_EXPIRED");
    }
    if (!response.ok) throw new Error("Błąd serwera");
    return response;
};

export const EmailAccountService = {
    fetchAccounts: async () => {
        const response = await fetch(`${API_URL}/list`, {
            method: 'GET',
            headers: getHeaders()
        });
        await handleResponse(response);
        return await response.json();
    },

    addAccount: async (accountData) => {
        const response = await fetch(`${API_URL}/add`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(accountData)
        });
        await handleResponse(response);
        return await response.json();
    },

    deleteAccount: async (emailAddress) => {
        const response = await fetch(`${API_URL}/delete?emailAddress=${encodeURIComponent(emailAddress)}`, {
            method: 'DELETE',
            headers: getHeaders()
        });

        await handleResponse(response);
        return await response.json();
    }
};