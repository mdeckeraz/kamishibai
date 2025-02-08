document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');

    registerForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Hide any existing messages
        errorMessage.classList.add('d-none');
        successMessage.classList.add('d-none');

        // Get form values
        const name = document.getElementById('name').value;
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Basic validation
        if (password !== confirmPassword) {
            showError('Passwords do not match');
            return;
        }

        if (password.length < 6) {
            showError('Password must be at least 6 characters long');
            return;
        }

        try {
            const response = await fetch('/api/accounts/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    name: name,
                    email: email,
                    password: password
                })
            });

            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.message || 'Failed to create account');
            }

            // Show success message
            showSuccess('Account created successfully! Redirecting to login...');
            
            // Clear form
            registerForm.reset();
            
            // Redirect to login page after 2 seconds
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);

        } catch (error) {
            showError(error.message || 'An error occurred while creating your account');
        }
    });

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.remove('d-none');
    }

    function showSuccess(message) {
        successMessage.textContent = message;
        successMessage.classList.remove('d-none');
    }
});
