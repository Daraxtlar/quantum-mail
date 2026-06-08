const API_URL = 'http://localhost:8080/api/auth';

export const authService = {
    login: async (username, password) => {
        const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, password})
        });
        if (!response.ok) throw new Error('Invalid Credentials');

        const data = await response.json();
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data));
        return data;
    },

    register: async (username, password, email) => {
        const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, password, email})
        });
        if (!response.ok) {
            throw new Error('Registration failed');
        }
        return await response.json();
    }
};