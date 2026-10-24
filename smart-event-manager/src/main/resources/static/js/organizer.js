// Organizer dashboard logic

let allEvents = [];

document.addEventListener('DOMContentLoaded', refreshAll);

async function refreshAll() {
  await loadEvents();
  loadStats();
  loadTasks();
  loadParticipants();
}

// ---------------- Overview ----------------
async function loadStats() {
  try {
    const s = await api('/api/stats');
    const cards = [
      ['Active events', s.activeEvents], ['Confirmed seats', s.confirmed],
      ['On waitlists', s.waitlisted], ['Attended', s.attended],
      ['Cancelled', s.cancelled], ['Pending tasks', s.pendingTasks],
    ];
    document.getElementById('statCards').innerHTML = cards.map(([label, n]) => `
      <div class="col-6 col-md-4 col-lg-2">
        <div class="card stat-card text-center"><div class="card-body">
          <div class="num">${n}</div><div class="small text-muted">${label}</div>
        </div></div>
      </div>`).join('');
  } catch (err) { showToast(err.message, 'danger'); }
}

// ---------------- Events ----------------
async function loadEvents() {
  try {
    allEvents = await api('/api/events');
  } catch (err) { return showToast(err.message, 'danger'); }

  document.getElementById('eventRows').innerHTML = allEvents.map(e => {
    const active = e.status === 'ACTIVE';
    return `<tr>
      <td><strong>${esc(e.name)}</strong><br><span class="badge text-bg-secondary">${esc(e.category || '')}</span></td>
      <td>${fmtDate(e.eventDate)}</td>
      <td>${esc(e.venue || '')}</td>
      <td>${e.confirmed} / ${e.capacity} <span class="text-muted small">(${e.seatsLeft} left)</span></td>
      <td>${e.waitlisted}</td>
      <td><span class="badge text-bg-${active ? 'success' : 'danger'}">${e.status}</span></td>
      <td class="text-end text-nowrap">${active ? `
        <button class="btn btn-outline-primary btn-sm" onclick="editEvent(${e.id})">Edit</button>
        <button class="btn btn-outline-danger btn-sm" onclick="cancelEvent(${e.id})">Cancel</button>` : ''}
      </td></tr>`;
  }).join('') || '<tr><td colspan="7" class="text-muted">No events yet.</td></tr>';

  const active = allEvents.filter(e => e.status === 'ACTIVE');
  const options = active.map(e => `<option value="${e.id}">${esc(e.name)}</option>`).join('');
  const pe = document.getElementById('peopleEvent');
  const keep = pe.value;
  pe.innerHTML = allEvents.map(e => `<option value="${e.id}">${esc(e.name)}${e.status !== 'ACTIVE' ? ' (cancelled)' : ''}</option>`).join('');
  if (keep) pe.value = keep;
  document.getElementById('taskEvent').innerHTML = '<option value="">General (no event)</option>' + options;
}

async function saveEvent(ev) {
  ev.preventDefault();
  const id = document.getElementById('evId').value;
  const body = {
    name: document.getElementById('evName').value,
    category: document.getElementById('evCategory').value,
    eventDate: document.getElementById('evDate').value,
    venue: document.getElementById('evVenue').value,
    capacity: parseInt(document.getElementById('evCapacity').value, 10),
    description: document.getElementById('evDesc').value,
  };
  try {
    await api(id ? '/api/events/' + id : '/api/events', { method: id ? 'PUT' : 'POST', body });
    showToast(id ? 'Event updated' : 'Event created');
    resetEventForm();
    refreshAll();
  } catch (err) { showToast(err.message, 'danger'); }
}

function editEvent(id) {
  const e = allEvents.find(x => x.id === id);
  if (!e) return;
  document.getElementById('evId').value = e.id;
  document.getElementById('evName').value = e.name;
  document.getElementById('evCategory').value = e.category || 'Other';
  document.getElementById('evDate').value = e.eventDate;
  document.getElementById('evVenue').value = e.venue || '';
  document.getElementById('evCapacity').value = e.capacity;
  document.getElementById('evDesc').value = e.description || '';
  document.getElementById('formTitle').textContent = 'Edit event';
  document.getElementById('evSaveBtn').textContent = 'Update';
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function resetEventForm() {
  document.getElementById('eventForm').reset();
  document.getElementById('evId').value = '';
  document.getElementById('formTitle').textContent = 'Create event';
  document.getElementById('evSaveBtn').textContent = 'Create';
}

async function cancelEvent(id) {
  if (!confirm('Cancel this event? All registrations will be cancelled.')) return;
  try {
    await api('/api/events/' + id, { method: 'DELETE' });
    showToast('Event cancelled', 'warning');
    refreshAll();
  } catch (err) { showToast(err.message, 'danger'); }
}

// ---------------- Participants ----------------
async function loadParticipants() {
  const id = document.getElementById('peopleEvent').value;
  const rows = document.getElementById('peopleRows');
  if (!id) { rows.innerHTML = '<tr><td colspan="5" class="text-muted">Select an event.</td></tr>'; return; }
  try {
    const [people, ev] = await Promise.all([api(`/api/events/${id}/registrations`), api('/api/events/' + id)]);
    document.getElementById('peopleSummary').textContent =
      `${ev.confirmed} confirmed / ${ev.capacity} capacity · ${ev.seatsLeft} seats left · ${ev.waitlisted} on waitlist`;
    rows.innerHTML = people.map(r => {
      let badge = 'secondary';
      if (r.status === 'CONFIRMED') badge = 'success';
      if (r.status === 'WAITLISTED') badge = 'warning';
      if (r.status === 'CANCELLED') badge = 'danger';
      const label = r.status === 'WAITLISTED' ? `WAITLISTED #${r.waitlistPosition}` : r.status;
      const attend = r.status === 'CONFIRMED'
        ? `<div class="form-check form-switch"><input class="form-check-input" type="checkbox" ${r.attended ? 'checked' : ''}
             onchange="setAttendance(${r.id}, this.checked)"><label class="form-check-label small">${r.attended ? 'Present' : 'Absent'}</label></div>`
        : '<span class="text-muted">-</span>';
      const cancel = r.status === 'CANCELLED' ? '' :
        `<button class="btn btn-outline-danger btn-sm" onclick="cancelRegistration(${r.id})">Cancel</button>`;
      return `<tr><td>${esc(r.studentName)}</td><td>${esc(r.studentEmail)}</td>
        <td><span class="badge text-bg-${badge}">${esc(label)}</span></td><td>${attend}</td>
        <td class="text-end">${cancel}</td></tr>`;
    }).join('') || '<tr><td colspan="5" class="text-muted">No registrations yet.</td></tr>';
  } catch (err) { showToast(err.message, 'danger'); }
}

async function setAttendance(id, present) {
  try {
    await api(`/api/registrations/${id}/attend?present=${present}`, { method: 'PUT' });
    loadParticipants(); loadStats(); loadUndoLabel();
  } catch (err) { showToast(err.message, 'danger'); loadParticipants(); }
}

async function cancelRegistration(id) {
  if (!confirm('Cancel this registration? The next person on the waitlist (if any) will be promoted.')) return;
  try {
    await api('/api/registrations/' + id, { method: 'DELETE' });
    showToast('Registration cancelled');
    loadEvents(); loadParticipants(); loadStats();
  } catch (err) { showToast(err.message, 'danger'); }
}

// ---------------- Tasks ----------------
async function loadTasks() {
  try {
    const t = await api('/api/tasks');
    const evName = id => { const e = allEvents.find(x => x.id === id); return e ? e.name : 'General'; };
    document.getElementById('pendingTasks').innerHTML = t.pending.map((x, i) => `
      <div class="card mb-2 priority-${x.priority}"><div class="card-body py-2 d-flex justify-content-between align-items-center">
        <div><strong>${i === 0 ? '⭐ ' : ''}${esc(x.title)}</strong>
          <div class="small text-muted">${esc(evName(x.eventId))}</div></div>
        <span class="badge text-bg-dark">P${x.priority}</span>
      </div></div>`).join('') || '<div class="text-muted">No pending tasks 🎉</div>';
    document.getElementById('doneTasks').innerHTML = t.completed.map(x => `
      <div class="card mb-2"><div class="card-body py-2 text-muted"><s>${esc(x.title)}</s></div></div>`).join('')
      || '<div class="text-muted">Nothing completed yet.</div>';
  } catch (err) { showToast(err.message, 'danger'); }
  loadUndoLabel();
}

async function addTask(ev) {
  ev.preventDefault();
  const evId = document.getElementById('taskEvent').value;
  try {
    await api('/api/tasks', { method: 'POST', body: {
      title: document.getElementById('taskTitle').value,
      priority: parseInt(document.getElementById('taskPriority').value, 10),
      eventId: evId ? parseInt(evId, 10) : null,
    }});
    document.getElementById('taskTitle').value = '';
    loadTasks(); loadStats();
  } catch (err) { showToast(err.message, 'danger'); }
}

async function processNext() {
  try {
    const t = await api('/api/tasks/next', { method: 'POST' });
    showToast('Completed: ' + t.title);
    loadTasks(); loadStats();
  } catch (err) { showToast(err.message, 'warning'); }
}

async function loadUndoLabel() {
  try {
    const u = await api('/api/undo');
    const btn = document.getElementById('undoBtn');
    btn.disabled = !u.next;
    btn.textContent = u.next ? '↶ Undo: ' + u.next : '↶ Nothing to undo';
  } catch (e) { /* ignore */ }
}

async function undoLast() {
  try {
    const r = await api('/api/undo', { method: 'POST' });
    showToast('Undone: ' + r.undone, 'info');
    refreshAll();
  } catch (err) { showToast(err.message, 'warning'); }
}
