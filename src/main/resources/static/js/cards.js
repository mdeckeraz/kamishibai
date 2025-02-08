async function toggleCardState(cardId) {
    try {
        const response = await fetch(`/api/cards/${cardId}/toggle`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to toggle card state');
        }

        // Reload the page to show the updated state
        window.location.reload();
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to toggle card state');
    }
}

async function viewAuditLog(cardId) {
    try {
        const response = await fetch(`/api/cards/${cardId}/audit`);
        if (!response.ok) {
            throw new Error('Failed to fetch audit log');
        }

        const auditLog = await response.json();
        const auditLogContent = document.getElementById('auditLogContent');
        
        // Create the audit log HTML
        let html = '<div class="list-group">';
        auditLog.forEach(entry => {
            const date = new Date(entry.changedAt).toLocaleString();
            html += `
                <div class="list-group-item">
                    <div class="d-flex justify-content-between">
                        <span>${entry.previousState} → ${entry.newState}</span>
                        <small class="text-muted">${date}</small>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        
        auditLogContent.innerHTML = html;
        
        // Show the modal
        const modal = new bootstrap.Modal(document.getElementById('auditLogModal'));
        modal.show();
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to fetch audit log');
    }
}
