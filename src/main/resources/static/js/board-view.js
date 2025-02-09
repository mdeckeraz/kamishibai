document.addEventListener('DOMContentLoaded', function() {
    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

    function handleCardClick(event) {
        // Don't toggle if clicking the edit button or its parent
        if (event.target.closest('.btn-outline-secondary')) {
            return;
        }

        const card = event.currentTarget;
        const cardId = card.getAttribute('data-card-id');
        const boardId = card.getAttribute('data-board-id');

        // Send request to toggle card state
        fetch(`/boards/${boardId}/cards/${cardId}/toggle`, {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            // Get the target container based on new state
            const targetContainer = data.state === 'GREEN' 
                ? document.querySelector('.card-header-green').closest('.card').querySelector('.card-list')
                : document.querySelector('.card-header-red').closest('.card').querySelector('.card-list');

            // Update card appearance
            card.classList.remove('card-red', 'card-green');
            card.classList.add(`card-${data.state.toLowerCase()}`);

            // Move card to new container
            targetContainer.appendChild(card);

            // Check if containers are empty and update empty state messages
            updateEmptyStateMessages();
        })
        .catch(error => {
            console.error('Error:', error);
            // Optionally show an error message to the user
        });
    }

    function updateEmptyStateMessages() {
        const containers = [
            document.querySelector('.card-header-red').closest('.card').querySelector('.card-list'),
            document.querySelector('.card-header-green').closest('.card').querySelector('.card-list')
        ];

        containers.forEach(container => {
            const cards = container.querySelectorAll('.card-red, .card-green');
            const emptyMessage = container.querySelector('.text-center.text-muted');
            
            if (cards.length === 0 && !emptyMessage) {
                const message = document.createElement('div');
                message.className = 'text-center text-muted py-4';
                message.innerHTML = `
                    <i class="bi bi-inbox fs-2 d-block mb-2"></i>
                    ${container.closest('.card').querySelector('.card-header').textContent.trim() === 'Problems' 
                        ? 'No problem cards yet' 
                        : 'No solution cards yet'}
                `;
                container.appendChild(message);
            } else if (cards.length > 0 && emptyMessage) {
                emptyMessage.remove();
            }
        });
    }

    // Add click handlers to all cards
    document.querySelectorAll('.card-red, .card-green').forEach(card => {
        card.style.cursor = 'pointer';
        card.addEventListener('click', handleCardClick);
    });
});
