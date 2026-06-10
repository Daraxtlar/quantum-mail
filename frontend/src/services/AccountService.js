const API_URL = "http://localhost:8080/api/account";

const getHeaders = () => {
    const token = localStorage.getItem("token");
    return {
        "Authorization": `Bearer ${token}`,
        "Content-type": "application/json"
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

export const AccountService = {
    changePassword: async (oldPassword, newPassword) => {
        const response = await fetch(`${API_URL}/change-password`, {
            method: "POST",
            headers: getHeaders(),
            body: JSON.stringify({oldPassword, newPassword})
        });
        await handleResponse(response);
        return response.text();
    },

    deleteAccount: async (password) => {
        const response = await fetch(`${API_URL}/delete`, {
            method: "DELETE",
            headers: getHeaders(),
            body: JSON.stringify({password})
        });
        await handleResponse(response);
        return response.text();
    }
}