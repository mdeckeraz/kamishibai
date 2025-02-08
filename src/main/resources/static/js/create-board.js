document.getElementById('createBoardForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    errorMessage.classList.add('d-none');
    successMessage.classList.add('d-none');
    
    const boardData = {
        name: document.getElementById('name').value,
        description: document.getElementById('description').value
    };
    
    try {
        const response = await fetch('/api/boards', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(boardData)
        });
        
        if (response.ok) {
            successMessage.textContent = 'Board created successfully!';
            successMessage.classList.remove('d-none');
            // Redirect to dashboard after a short delay
            setTimeout(() => {
                window.location.href = '/dashboard';
            }, 1500);
        } else {
            const data = await response.json();
            errorMessage.textContent = data.message || 'Failed to create board';
            errorMessage.classList.remove('d-none');
        }
    } catch (error) {
        errorMessage.textContent = 'An error occurred while creating the board';
        errorMessage.classList.remove('d-none');
    }
});
