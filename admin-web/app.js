// Firebase Configuration
const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();

// Global variables
let currentDepartment = '';
let currentServingQueue = null;
let unsubscribeListener = null;

// Admin passwords (In production, use proper authentication)
const ADMIN_PASSWORDS = {
    'Finance': 'finance2026',
    'Registrar': 'registrar2026'
};

// Login function
function login() {
    const department = document.getElementById('departmentSelect').value;
    const password = document.getElementById('adminPassword').value;
    const errorElement = document.getElementById('loginError');

    if (!department) {
        errorElement.textContent = 'Please select a department';
        return;
    }

    if (!password) {
        errorElement.textContent = 'Please enter password';
        return;
    }

    if (ADMIN_PASSWORDS[department] === password) {
        currentDepartment = department;
        document.getElementById('loginScreen').classList.remove('active');
        document.getElementById('queueScreen').classList.add('active');
        document.getElementById('departmentTitle').textContent = `${department} Queue Management`;
        
        // Set current date
        const dateOptions = { year: 'numeric', month: 'long', day: 'numeric' };
        document.getElementById('currentDate').textContent = new Date().toLocaleDateString('en-US', dateOptions);
        
        // Start listening to queue updates
        startQueueListener();
    } else {
        errorElement.textContent = 'Invalid password';
    }
}

// Logout function
function logout() {
    if (unsubscribeListener) {
        unsubscribeListener();
    }
    currentDepartment = '';
    currentServingQueue = null;
    document.getElementById('queueScreen').classList.remove('active');
    document.getElementById('loginScreen').classList.add('active');
    document.getElementById('adminPassword').value = '';
    document.getElementById('loginError').textContent = '';
}

// Start listening to queue updates
function startQueueListener() {
    const today = new Date().toISOString().split('T')[0];
    
    unsubscribeListener = db.collection('appointments')
        .where('department', '==', currentDepartment)
        .where('appointmentDate', '==', today)
        .onSnapshot((snapshot) => {
            const queues = [];
            snapshot.forEach((doc) => {
                queues.push({ id: doc.id, ...doc.data() });
            });
            
            updateQueueDisplay(queues);
        }, (error) => {
            console.error('Error listening to queues:', error);
        });
}

// Update queue display
function updateQueueDisplay(queues) {
    // Sort by timestamp
    queues.sort((a, b) => {
        if (a.timestamp && b.timestamp) {
            return a.timestamp.toMillis() - b.timestamp.toMillis();
        }
        return 0;
    });

    // Find serving queue
    const servingQueue = queues.find(q => q.status === 'Serving');
    
    // Find pending queues
    const pendingQueues = queues.filter(q => q.status === 'Pending');
    
    // Find completed queues
    const completedQueues = queues.filter(q => q.status === 'Completed');

    // Update current serving
    if (servingQueue) {
        currentServingQueue = servingQueue;
        displayCurrentServing(servingQueue);
        document.getElementById('completeBtn').disabled = false;
        document.getElementById('skipBtn').disabled = false;
    } else {
        currentServingQueue = null;
        document.getElementById('currentQueueNumber').textContent = '---';
        document.getElementById('currentQueueDetails').innerHTML = '<p>No queue currently being served</p>';
        document.getElementById('completeBtn').disabled = true;
        document.getElementById('skipBtn').disabled = true;
    }

    // Update next in queue
    if (pendingQueues.length > 0) {
        displayNextQueue(pendingQueues[0]);
        document.getElementById('nextBtn').disabled = false;
    } else {
        document.getElementById('nextQueueNumber').textContent = '---';
        document.getElementById('nextQueueDetails').innerHTML = '<p>No pending queues</p>';
        document.getElementById('nextBtn').disabled = true;
    }

    // Update pending list
    displayPendingList(pendingQueues);

    // Update statistics
    document.getElementById('totalToday').textContent = queues.length;
    document.getElementById('completedToday').textContent = completedQueues.length;
    document.getElementById('pendingToday').textContent = pendingQueues.length;
    document.getElementById('pendingCount').textContent = pendingQueues.length;
}

// Display current serving queue
function displayCurrentServing(queue) {
    const queueNum = queue.queueNumber.split('-').pop();
    document.getElementById('currentQueueNumber').textContent = queueNum;
    
    let detailsHTML = `
        <p><strong>Queue:</strong> ${queue.queueNumber}</p>
        <p><strong>Transaction:</strong> ${queue.transactionType || 'N/A'}</p>
        <p><strong>Time:</strong> ${queue.appointmentTime || 'N/A'}</p>
    `;
    
    document.getElementById('currentQueueDetails').innerHTML = detailsHTML;
}

// Display next queue
function displayNextQueue(queue) {
    const queueNum = queue.queueNumber.split('-').pop();
    document.getElementById('nextQueueNumber').textContent = queueNum;
    
    let detailsHTML = `
        <p><strong>Queue:</strong> ${queue.queueNumber}</p>
        <p><strong>Transaction:</strong> ${queue.transactionType || 'N/A'}</p>
    `;
    
    document.getElementById('nextQueueDetails').innerHTML = detailsHTML;
}

// Display pending list
function displayPendingList(pendingQueues) {
    const listElement = document.getElementById('pendingQueueList');
    
    if (pendingQueues.length === 0) {
        listElement.innerHTML = '<p class="empty-message">No pending queues</p>';
        return;
    }

    let html = '';
    pendingQueues.forEach((queue, index) => {
        const queueNum = queue.queueNumber.split('-').pop();
        html += `
            <div class="queue-item">
                <div class="queue-item-number">${queueNum}</div>
                <div class="queue-item-details">
                    <p><strong>${queue.queueNumber}</strong></p>
                    <p>${queue.transactionType || 'N/A'}</p>
                </div>
                <div style="color: #999; font-size: 14px;">Position: ${index + 1}</div>
            </div>
        `;
    });
    
    listElement.innerHTML = html;
}

// Call next queue
async function callNext() {
    const today = new Date().toISOString().split('T')[0];
    
    try {
        // Get the first pending queue
        const snapshot = await db.collection('appointments')
            .where('department', '==', currentDepartment)
            .where('appointmentDate', '==', today)
            .where('status', '==', 'Pending')
            .orderBy('timestamp', 'asc')
            .limit(1)
            .get();

        if (!snapshot.empty) {
            const doc = snapshot.docs[0];
            await db.collection('appointments').doc(doc.id).update({
                status: 'Serving'
            });
            
            console.log('Called next queue:', doc.data().queueNumber);
        }
    } catch (error) {
        console.error('Error calling next queue:', error);
        alert('Error calling next queue: ' + error.message);
    }
}

// Complete current queue
async function completeQueue() {
    if (!currentServingQueue) return;
    
    try {
        await db.collection('appointments').doc(currentServingQueue.id).update({
            status: 'Completed'
        });
        
        console.log('Completed queue:', currentServingQueue.queueNumber);
    } catch (error) {
        console.error('Error completing queue:', error);
        alert('Error completing queue: ' + error.message);
    }
}

// Skip current queue
async function skipQueue() {
    if (!currentServingQueue) return;
    
    if (confirm('Are you sure you want to skip this queue? It will be moved back to pending.')) {
        try {
            await db.collection('appointments').doc(currentServingQueue.id).update({
                status: 'Pending'
            });
            
            console.log('Skipped queue:', currentServingQueue.queueNumber);
        } catch (error) {
            console.error('Error skipping queue:', error);
            alert('Error skipping queue: ' + error.message);
        }
    }
}
