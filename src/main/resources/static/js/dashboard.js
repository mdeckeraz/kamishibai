async function loadBoards() {
    try {
        const response = await fetch('/api/boards');
        if (response.ok) {
            const boards = await response.json();
            displayBoards(boards);
        } else {
            console.error('Failed to load boards');
        }
    } catch (error) {
        console.error('Error loading boards:', error);
    }
}

function displayBoards(boards) {
    const boardsList = document.getElementById('boardsList');
    if (boards.length === 0) {
        boardsList.innerHTML = `
            <div class="col-12">
                <div class="alert alert-info">
                    You don't have any boards yet. Create your first board to get started!
                </div>
            </div>
        `;
        return;
    }

    boardsList.innerHTML = boards.map(board => `
        <div class="col-md-4 mb-4">
            <div class="card shadow-sm">
                <div class="card-body">
                    <h5 class="card-title">${board.name}</h5>
                    <p class="card-text text-muted">
                        ${board.description || 'No description'}
                    </p>
                    <p class="card-text">
                        <small class="text-muted">
                            Created: ${new Date(board.createdAt).toLocaleDateString()}
                        </small>
                    </p>
                    <div class="d-flex justify-content-between align-items-center">
                        <a href="/boards/${board.id}" class="btn btn-primary">View Board</a>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

// Load boards when the page loads
document.addEventListener('DOMContentLoaded', loadBoards);
