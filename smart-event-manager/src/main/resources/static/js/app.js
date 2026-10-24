// Student portal logic

let currentEvent = null;
let registerModal = null;
let searchTimer = null;

document.addEventListener('DOMContentLoaded', () => {
  registerModal = new bootstrap.Modal(document.getElementById('registerModal'));
  document.getElementById('searchBox').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadEvents, 250);
  });
  document.getElementById('categoryFilter').addEventListener('change', loadEvents);
  const saved = localStorage.getItem('studentEmail');
  if (saved) document.getElementById('myEmail').value = saved;
  loadEvents();
});

async function loadEvents() {
  const q = encodeURIComponent(document.getElementById('searchBox').value);
  const c = encodeURIComponent(document.getElementById('categoryFilter').value);
  try {
    const events = await api(`/api/events?search=${q}&category=${c}`);
    renderEvents(events.filter(e => e.status === 'ACTIVE'));
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function renderEvents(events) {
  const grid = document.getElementById('eventGrid');
  if (!events.length) {
    grid.innerHTML = '<div class="col-12"><div class="alert alert-light">No events found.</div></div>';
    return;
  }
  grid.innerHTML = events.map(e => {
    const full = e.seatsLeft === 0;
    const seats = full
      ? `<span class="badge text-bg-danger">Full</span> <span class="small text-muted">${e.waitlisted} on waitlist</span>`
      : `<span class="badge text-bg-success">${e.seatsLeft} of ${e.capacity} seats left</span>`;
    const btn = full
      ? `<button class="btn btn-warning btn-sm" onclick="openRegister(${e.id})">Join waitlist</button>`
      : `<button class="btn btn-primary btn-sm" onclick="openRegister(${e.id})">Register</button>`;
    return `
      <div class="col-md-6 col-lg-4">
        <div class="card event-card h-100">
          <div class="card-body d-flex flex-column">
            <span class="badge text-bg-secondary category-badge align-self-start mb-2">${esc(e.category || 'General')}</span>
            <h5 class="card-title">${esc(e.name)}</h5>
            <p class="card-text text-muted small flex-grow-1">${esc(e.description || '')}</p>
            <ul class="list-unstyled small mb-3">
              <li>📅 ${fmtDate(e.eventDate)}</li>
              <li>📍 ${esc(e.venue || 'TBA')}</li>
            </ul>
            <div class="d-flex justify-content-between align-items-center">
              <div>${seats}</div>${btn}
            </div>
          </div>
        </div>
      </div>`;
  }).join('');
}

async function openRegister(id) {
  try {
    currentEvent = await api('/api/events/' + id);
  } catch (err) {
    return showToast(err.message, 'danger');
  }
  const full = currentEvent.seatsLeft === 0;
  document.getElementById('registerTitle').textContent = (full ? 'Join waitlist: ' : 'Register: ') + currentEvent.name;
  document.getElementById('registerNote').textContent = full
    ? 'This event is full. You will be placed in the queue and promoted automatically if a seat opens up.'
    : currentEvent.seatsLeft + ' seat(s) remaining.';
  document.getElementById('registerBtn').textContent = full ? 'Join waitlist' : 'Register';
  document.getElementById('regEmail').value = localStorage.getItem('studentEmail') || '';
  registerModal.show();
}

async function submitRegistration(ev) {
  ev.preventDefault();
  const name = document.getElementById('regName').value;
  const email = document.getElementById('regEmail').value;
  try {
    const res = await api(`/api/events/${currentEvent.id}/register`, { method: 'POST', body: { name, email } });
    localStorage.setItem('studentEmail', email);
    document.getElementById('myEmail').value = email;
    registerModal.hide();
    showToast(res.message, res.status === 'CONFIRMED' ? 'success' : 'warning');
    loadEvents();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function loadMine() {
  const email = document.getElementById('myEmail').value.trim();
  const box = document.getElementById('mineList');
  if (!email) return showToast('Enter your email first', 'warning');
  localStorage.setItem('studentEmail', email);
  try {
    const regs = await api('/api/registrations/status?email=' + encodeURIComponent(email));
    if (!regs.length) {
      box.innerHTML = '<div class="alert alert-light">No registrations found for this email.</div>';
      return;
    }
    box.innerHTML = `<table class="table bg-white rounded shadow-sm align-middle">
      <thead><tr><th>Event</th><th>Status</th><th></th></tr></thead><tbody>` +
      regs.map(r => {
        let badge = 'secondary', label = r.status;
        if (r.status === 'CONFIRMED') badge = 'success';
        if (r.status === 'WAITLISTED') { badge = 'warning'; label = 'WAITLISTED (position ' + r.waitlistPosition + ')'; }
        if (r.status === 'CANCELLED') badge = 'danger';
        const cancel = r.status === 'CANCELLED' ? '' :
          `<button class="btn btn-outline-danger btn-sm" onclick="cancelMine(${r.id})">Cancel</button>`;
        return `<tr><td>${esc(r.eventName)}</td><td><span class="badge text-bg-${badge}">${esc(label)}</span></td><td class="text-end">${cancel}</td></tr>`;
      }).join('') + '</tbody></table>';
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function cancelMine(id) {
  if (!confirm('Cancel this registration?')) return;
  try {
    await api('/api/registrations/' + id, { method: 'DELETE' });
    showToast('Registration cancelled');
    loadMine();
    loadEvents();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}
