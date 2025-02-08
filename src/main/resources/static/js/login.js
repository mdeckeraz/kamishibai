document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');

    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Hide any existing error messages
        errorMessage.classList.add('d-none');

        // Get form values
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email: email,
                    password: password
                })
            });

            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.message || 'Invalid email or password');
            }

            const data = await response.json();
            
            // Store the token in localStorage
            localStorage.setItem('token', data.token);
            
            // Redirect to dashboard
            window.location.href = '/dashboard';

        } catch (error) {
            showError(error.message || 'An error occurred while logging in');
        }
    });

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.remove('d-none');
    }
});
