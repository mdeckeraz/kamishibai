document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form');
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');

    // Get CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        if (errorMessage) errorMessage.classList.add('d-none');
        if (successMessage) successMessage.classList.add('d-none');
        
        const boardId = document.getElementById('boardId').value;
        const cardId = document.getElementById('cardId')?.value;
        const isEdit = cardId != null && cardId !== '';
        
        // Get the time input value (HH:mm format)
        const resetTimeInput = document.getElementById('resetTime').value;
        if (!resetTimeInput.match(/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/)) {
            if (errorMessage) {
                errorMessage.textContent = 'Please enter a valid time in 24-hour format (HH:mm)';
                errorMessage.classList.remove('d-none');
            }
            return;
        }
        
        const cardData = {
            title: document.getElementById('title').value,
            details: document.getElementById('details').value,
            state: document.getElementById('state').value,
            resetTime: resetTimeInput,
            position: 0 // We'll handle positioning server-side
        };
        
        const url = isEdit 
            ? `/api/boards/${boardId}/cards/${cardId}`
            : `/api/boards/${boardId}/cards`;
            
        try {
            const headers = {
                'Content-Type': 'application/json'
            };
            headers[csrfHeader] = csrfToken;

            console.log('Sending request to:', url);
            console.log('Request data:', cardData);

            const response = await fetch(url, {
                method: isEdit ? 'PUT' : 'POST',
                headers: headers,
                body: JSON.stringify(cardData)
            });
            
            const data = await response.json();
            console.log('Response:', data);
            
            if (response.ok) {
                if (successMessage) {
                    successMessage.textContent = data.message || `Card ${isEdit ? 'updated' : 'created'} successfully!`;
                    successMessage.classList.remove('d-none');
                }
                
                // Clear form
                form.reset();
                
                // Redirect to board view after a short delay
                setTimeout(() => {
                    window.location.href = `/boards/${boardId}`;
                }, 1000);
            } else {
                if (errorMessage) {
                    errorMessage.textContent = data.message || `Failed to ${isEdit ? 'update' : 'create'} card`;
                    errorMessage.classList.remove('d-none');
                }
            }
        } catch (error) {
            console.error('Error:', error);
            if (errorMessage) {
                errorMessage.textContent = `An error occurred while ${isEdit ? 'updating' : 'creating'} the card`;
                errorMessage.classList.remove('d-none');
            }
        }
    });
});
