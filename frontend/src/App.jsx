import { useEffect, useState } from 'react';
import { api } from './api';

const roleHome = { CUSTOMER: 'customerDashboard', WORKER: 'workerDashboard', ADMIN: 'adminDashboard' };
const roleNames = { CUSTOMER: 'Customer', WORKER: 'Worker', ADMIN: 'Admin' };
const friendlyError = (error) => error.response?.data?.message || (error.request
  ? 'Cannot reach the backend. Start it in the backend folder, then refresh this page.'
  : 'Something went wrong. Please try again.');
const money = (value) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value || 0);

function getSavedUser() {
  try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch { return null; }
}

export default function App() {
  const [user, setUser] = useState(getSavedUser);
  const [page, setPage] = useState(() => getSavedUser() ? roleHome[getSavedUser()?.role] || 'home' : 'home');
  const [services, setServices] = useState([]);
  const [requests, setRequests] = useState([]);
  const [bids, setBids] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [users, setUsers] = useState([]);
  const [adminSetupAvailable, setAdminSetupAvailable] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [adminTab, setAdminTab] = useState('requests');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  function navigate(nextPage) { setError(''); setPage(nextPage); window.scrollTo(0, 0); }
  function showNotice(text) { setNotice(text); window.setTimeout(() => setNotice(''), 3500); }

  useEffect(() => {
    api.get('/auth/admin-setup')
      .then(({ data }) => setAdminSetupAvailable(data.available))
      .catch(() => setAdminSetupAvailable(false));
  }, []);

  async function refreshRequests() {
    const { data } = await api.get('/requests');
    setRequests(data);
    return data;
  }

  useEffect(() => {
    if (!user) return;
    let active = true;
    setLoading(true);
    setError('');
    async function loadPageData() {
      try {
        if (page === 'createRequest') {
          const response = await api.get('/services');
          if (active) setServices(response.data);
        } else if (['customerDashboard', 'myRequests'].includes(page)) {
          await refreshRequests();
        } else if (page === 'workerDashboard') {
          const [requestResponse, jobResponse] = await Promise.all([api.get('/requests'), api.get('/requests/my-jobs')]);
          if (active) { setRequests(requestResponse.data); setJobs(jobResponse.data); }
        } else if (page === 'availableRequests') {
          await refreshRequests();
        } else if (page === 'myJobs') {
          const response = await api.get('/requests/my-jobs');
          if (active) setJobs(response.data);
        } else if (page === 'adminDashboard') {
          const [requestResponse, userResponse, serviceResponse] = await Promise.all([
            api.get('/requests'), api.get('/users'), api.get('/services')
          ]);
          if (active) {
            setRequests(requestResponse.data);
            setUsers(userResponse.data);
            setServices(serviceResponse.data);
          }
        }
      } catch (loadError) {
        if (active) setError(friendlyError(loadError));
      } finally {
        if (active) setLoading(false);
      }
    }
    loadPageData();
    return () => { active = false; };
  }, [page, user]);

  async function authenticate(event, action) {
    event.preventDefault(); setError('');
    const form = new FormData(event.currentTarget);
    const body = Object.fromEntries(form.entries());
    if (action === 'login') delete body.name;
    try {
      const { data } = await api.post(`/auth/${action}`, body);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
      setUser(data.user);
      navigate(roleHome[data.user.role]);
    } catch (authError) { setError(friendlyError(authError)); }
  }

  function signOut() {
    localStorage.removeItem('token'); localStorage.removeItem('user');
    setUser(null); setRequests([]); setJobs([]); setBids([]); setSelectedRequest(null);
    navigate('home');
  }

  async function createRequest(event) {
    event.preventDefault(); setError('');
    const form = event.currentTarget;
    const body = Object.fromEntries(new FormData(form));
    try {
      await api.post('/requests', body);
      form.reset(); showNotice('Your service request has been created.'); navigate('myRequests');
    } catch (requestError) { setError(friendlyError(requestError)); }
  }

  async function openBids(request) {
    setError(''); setSelectedRequest(request); setBids([]); navigate('viewBids');
    try {
      const { data } = await api.get(`/bids/request/${request._id}`);
      setBids(data);
    } catch (bidError) { setError(friendlyError(bidError)); }
  }

  async function openWorkerRequest(request) {
    setError(''); setSelectedRequest(request); navigate('submitBid');
    try {
      const { data } = await api.get(`/requests/${request._id}`);
      setSelectedRequest(data);
    } catch (requestError) { setError(friendlyError(requestError)); }
  }

  async function submitBid(event) {
    event.preventDefault(); setError('');
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    try {
      await api.post('/bids', { ...values, request: selectedRequest._id, price: Number(values.price) });
      form.reset(); showNotice('Your bid has been submitted.'); navigate('availableRequests');
    } catch (bidError) { setError(friendlyError(bidError)); }
  }

  async function selectBid(bid) {
    setError('');
    try {
      await api.put(`/bids/${bid._id}/select`);
      showNotice('Worker selected. The request is now assigned.');
      await refreshRequests();
      navigate('myRequests');
    } catch (selectError) { setError(friendlyError(selectError)); }
  }

  async function createService(event) {
    event.preventDefault(); setError('');
    const form = event.currentTarget;
    const body = Object.fromEntries(new FormData(form));
    try {
      await api.post('/services', body);
      form.reset();
      const { data } = await api.get('/services'); setServices(data);
      showNotice('Service added.');
    } catch (serviceError) { setError(friendlyError(serviceError)); }
  }

  async function createAdmin(event) {
    event.preventDefault(); setError('');
    const form = event.currentTarget;
    const body = Object.fromEntries(new FormData(form));
    try {
      await api.post('/users/admins', body);
      form.reset();
      const { data } = await api.get('/users'); setUsers(data);
      showNotice('Admin account added.');
    } catch (adminError) { setError(friendlyError(adminError)); }
  }

  const customerNav = [['customerDashboard', 'Dashboard'], ['createRequest', 'Create request'], ['myRequests', 'My requests']];
  const workerNav = [['workerDashboard', 'Dashboard'], ['availableRequests', 'Available requests'], ['myJobs', 'My jobs']];
  const adminNav = [['adminDashboard', 'Admin dashboard']];
  const nav = !user ? [['home', 'Home']] : user.role === 'CUSTOMER' ? customerNav : user.role === 'WORKER' ? workerNav : adminNav;

  return <div className="site-shell">
    <header className="topbar">
      <button className="brand" onClick={() => navigate(user ? roleHome[user.role] : 'home')}><span className="brand-mark">S</span><span>Service<span className="brand-light">Hub</span></span></button>
      <nav className="main-nav" aria-label="Main navigation">
        {nav.map(([target, label]) => <button key={target} className={page === target ? 'nav-link active' : 'nav-link'} onClick={() => navigate(target)}>{label}</button>)}
      </nav>
      <div className="account-nav">{user ? <><span className="user-chip"><span className="avatar">{user.name?.[0]?.toUpperCase()}</span><span>{user.name}<small>{roleNames[user.role]}</small></span></span><button className="button button-quiet" onClick={signOut}>Sign out</button></> : <><button className="button button-quiet" onClick={() => navigate('login')}>Sign in</button><button className="button button-small" onClick={() => navigate('register')}>Create account</button></>}</div>
    </header>

    <main className={page === 'home' ? '' : 'page-container'}>
      {error && <div className="message message-error" role="alert">{error}</div>}
      {notice && <div className="message message-success" role="status">{notice}</div>}
      {loading && <div className="loading-line">Loading your information…</div>}

      {page === 'home' && <HomePage onLogin={() => navigate('login')} onRegister={() => navigate('register')} />}
      {page === 'login' && <AuthPage mode="login" onSubmit={(event) => authenticate(event, 'login')} onSwitch={() => navigate('register')} />}
      {page === 'register' && <AuthPage mode="register" adminSetupAvailable={adminSetupAvailable} onSubmit={(event) => authenticate(event, 'register')} onSwitch={() => navigate('login')} />}

      {page === 'customerDashboard' && <CustomerDashboard requests={requests} onNavigate={navigate} onViewBids={openBids} />}
      {page === 'createRequest' && <CreateRequestPage services={services} onSubmit={createRequest} onNavigate={navigate} />}
      {page === 'myRequests' && <MyRequestsPage requests={requests} onViewBids={openBids} />}
      {page === 'viewBids' && <ViewBidsPage request={selectedRequest} bids={bids} onSelect={selectBid} onBack={() => navigate('myRequests')} />}

      {page === 'workerDashboard' && <WorkerDashboard requests={requests} jobs={jobs} onNavigate={navigate} />}
      {page === 'availableRequests' && <AvailableRequestsPage requests={requests} onSelect={openWorkerRequest} />}
      {page === 'submitBid' && <SubmitBidPage request={selectedRequest} onSubmit={submitBid} onBack={() => navigate('availableRequests')} />}
      {page === 'myJobs' && <MyJobsPage jobs={jobs} />}

      {page === 'adminDashboard' && <AdminDashboard tab={adminTab} setTab={setAdminTab} users={users} requests={requests} services={services} onCreateService={createService} onCreateAdmin={createAdmin} />}
    </main>
    <footer className="footer">ServiceHub <span>·</span> Local services, made simple</footer>
  </div>;
}

function HomePage({ onLogin, onRegister }) {
  return <>
    <section className="hero"><div className="hero-copy"><span className="eyebrow">A simpler way to get it done</span><h1>Good help for your home, <span>right around the corner.</span></h1><p>Post a service request, compare bids from local workers, and choose the person who fits your needs.</p><div className="hero-actions"><button className="button" onClick={onRegister}>Get started</button><button className="button button-outline" onClick={onLogin}>Sign in</button></div><div className="hero-note"><span className="status-dot"/> Simple requests · Clear bids · Your choice</div></div><div className="hero-card"><span className="eyebrow">How it works</span><h2>A clear process from start to finish.</h2><div className="flow-step"><span>01</span><div><b>Customer creates a request</b><small>Describe the service and location.</small></div></div><div className="flow-step"><span>02</span><div><b>Workers submit bids</b><small>Compare prices and messages.</small></div></div><div className="flow-step"><span>03</span><div><b>Customer selects a worker</b><small>The selected offer is recorded.</small></div></div></div></section>
    <section className="home-features"><div><span>01</span><h3>Describe the work</h3><p>Choose a service and tell workers what you need.</p></div><div><span>02</span><h3>Compare offers</h3><p>Review bids and messages before you decide.</p></div><div><span>03</span><h3>Select your worker</h3><p>Pick the offer that works best for you.</p></div></section>
  </>;
}

function AuthPage({ mode, onSubmit, onSwitch, adminSetupAvailable }) {
  const registering = mode === 'register';
  return <section className="auth-layout"><div className="auth-aside"><span className="eyebrow">ServiceHub</span><h1>{registering ? 'Join your local service community.' : 'Welcome back.'}</h1><p>{registering ? 'Create a customer or worker account to continue.' : 'Sign in to manage requests, bids, and bookings.'}</p></div><div className="card auth-card"><span className="eyebrow">{registering ? 'Create account' : 'Sign in'}</span><h2>{registering ? 'Get started' : 'Your account'}</h2><form className="form-stack" onSubmit={onSubmit}>
    {registering && <label>Full name<input name="name" required placeholder="Your name" autoComplete="name" /></label>}
    {registering && <label>Account type<select name="role" defaultValue="CUSTOMER"><option value="CUSTOMER">Customer</option><option value="WORKER">Worker</option>{adminSetupAvailable && <option value="ADMIN">Initial admin</option>}</select></label>}
    <label>Email address<input name="email" type="email" required placeholder="you@example.com" autoComplete="email" /></label>
    <label>Password<input name="password" type="password" required minLength="6" placeholder="At least 6 characters" autoComplete={registering ? 'new-password' : 'current-password'} /></label>
    <button className="button button-full">{registering ? 'Create account' : 'Sign in'}</button>
  </form><p className="auth-switch">{registering ? 'Already have an account?' : 'New to ServiceHub?'} <button onClick={onSwitch}>{registering ? 'Sign in' : 'Create an account'}</button></p></div></section>;
}

function PageHeading({ eyebrow, title, description, action }) {
  return <div className="page-heading"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</div>;
}

function Stat({ label, value, note }) { return <div className="stat-card"><span>{label}</span><strong>{value}</strong><small>{note}</small></div>; }
function EmptyState({ title, text, action }) { return <div className="empty-state"><div className="empty-icon">＋</div><h3>{title}</h3><p>{text}</p>{action}</div>; }

function CustomerDashboard({ requests, onNavigate, onViewBids }) {
  const open = requests.filter((request) => request.status === 'OPEN').length;
  const selected = requests.filter((request) => request.status === 'SELECTED').length;
  return <><PageHeading eyebrow="Customer workspace" title="Your service dashboard" description="Create a request and compare offers from service professionals." action={<button className="button" onClick={() => onNavigate('createRequest')}>＋ Create request</button>} />
    <div className="stats-grid"><Stat label="My requests" value={requests.length} note="Total requests posted"/><Stat label="Open" value={open} note="Waiting for a worker"/><Stat label="Workers selected" value={selected} note="Requests with a selected bid"/></div>
    <section className="card section-card"><div className="section-title"><div><h2>Recent requests</h2><p>Your latest service activity</p></div><button className="text-button" onClick={() => onNavigate('myRequests')}>View all →</button></div>
      {requests.length ? requests.slice(0, 4).map((request) => <RequestRow key={request._id} request={request} action={<button className="button button-outline button-small" onClick={() => onViewBids(request)}>View bids</button>} />) : <EmptyState title="No requests yet" text="Create your first service request to get started." action={<button className="button button-small" onClick={() => onNavigate('createRequest')}>Create request</button>} />}
    </section>
  </>;
}

function CreateRequestPage({ services, onSubmit, onNavigate }) {
  const [serviceId, setServiceId] = useState('');
  const [suggestion, setSuggestion] = useState(null);

  function suggestService(description) {
    const text = description.toLowerCase();
    const options = [
      { name: 'AC Repair', keywords: ['air conditioner', 'air conditioning', 'cooling', 'aircon'] },
      { name: 'Plumbing', keywords: ['tap', 'faucet', 'plumb', 'pipe', 'leak', 'toilet', 'drain'] },
      { name: 'Electrical', keywords: ['fan', 'electric', 'wiring', 'light', 'switch', 'power'] },
      { name: 'Appliance Repair', keywords: ['washing machine', 'washer', 'refrigerator', 'fridge', 'microwave', 'appliance'] },
      { name: 'Home Cleaning', keywords: ['clean', 'cleaning', 'dust', 'sweep', 'house needs'] },
    ];
    const paddedText = ` ${text} `;
    const match = options.find((option) => (option.name === 'AC Repair' && /(^|\W)ac(\W|$)/.test(text))
      || option.keywords.some((keyword) => paddedText.includes(keyword)));
    if (!match) { setSuggestion(null); return; }

    const normalizedName = match.name.toLowerCase().replace(/[^a-z]/g, '');
    const service = services.find((item) => item.name.toLowerCase().replace(/[^a-z]/g, '') === normalizedName);
    setSuggestion({ name: match.name, service });
    if (service) setServiceId(service._id);
  }

  return <><PageHeading eyebrow="Customer workspace" title="Create a service request" description="Share what you need and local workers can send you bids." action={<button className="button button-outline" onClick={() => onNavigate('myRequests')}>My requests</button>} />
    <section className="card form-card"><form className="form-stack" onSubmit={onSubmit}>
      <label>Service<select name="service" required value={serviceId} onChange={(event) => setServiceId(event.target.value)}><option value="" disabled>Select a service</option>{services.map((service) => <option key={service._id} value={service._id}>{service.name}</option>)}</select></label>
      {services.length === 0 && <p className="form-help">There are no services yet. Ask an admin to add services from the Admin dashboard.</p>}
      <label>Describe the work<textarea name="description" required rows="5" placeholder="What needs to be fixed, cleaned, or installed?" onChange={(event) => suggestService(event.target.value)} /></label>
      {suggestion && <p className="service-suggestion">Suggested service: <strong>{suggestion.name}</strong>{suggestion.service ? ' — selected for you.' : ' — please choose the closest available service above.'}</p>}
      <label>Location<input name="location" required placeholder="Area or neighborhood" /></label>
      <div className="form-actions"><button className="button" disabled={!services.length}>Post request</button><button type="button" className="button button-quiet" onClick={() => onNavigate('myRequests')}>Cancel</button></div>
    </form></section>
  </>;
}

function MyRequestsPage({ requests, onViewBids }) {
  return <><PageHeading eyebrow="Customer workspace" title="My requests" description="Track each request and review the offers you receive." />
    <section className="card section-card">{requests.length ? requests.map((request) => <RequestRow key={request._id} request={request} action={<button className="button button-outline button-small" onClick={() => onViewBids(request)}>View bids</button>} />) : <EmptyState title="Nothing posted yet" text="Your requests will appear here after you create one." />}</section>
  </>;
}

function ViewBidsPage({ request, bids, onSelect, onBack }) {
  return <><PageHeading eyebrow="Customer workspace" title="Compare worker bids" description={request ? `${request.service?.name || 'Service'} · ${request.location || ''}` : 'Choose a request to view its offers.'} action={<button className="button button-outline" onClick={onBack}>← My requests</button>} />
    {request && <section className="card request-summary"><span className="eyebrow">Your request</span><h2>{request.description}</h2><p>{request.service?.name || 'Service'} <span>·</span> {request.location} <StatusBadge status={request.status}/></p></section>}
    <section className="card section-card"><div className="section-title"><div><h2>Offers</h2><p>Review prices and worker messages</p></div><span className="count-pill">{bids.length} {bids.length === 1 ? 'bid' : 'bids'}</span></div>
      {bids.length ? bids.map((bid) => <article className="bid-row" key={bid._id}><div className="worker-avatar">{bid.worker?.name?.[0]?.toUpperCase() || 'W'}</div><div className="bid-main"><h3>{bid.worker?.name || 'Service worker'}</h3><p>{bid.message}</p><span className="status-text">Bid status: {bid.status}</span></div><div className="bid-side"><strong>{money(bid.price)}</strong>{request?.status === 'OPEN' && bid.status === 'PENDING' && <button className="button button-small" onClick={() => onSelect(bid)}>Select worker</button>}</div></article>) : <EmptyState title="No bids yet" text="Workers can see open requests and submit their offers." />}
    </section>
  </>;
}

function WorkerDashboard({ requests, jobs, onNavigate }) {
  return <><PageHeading eyebrow="Worker workspace" title="Your worker dashboard" description="Find service requests and review the jobs customers have selected you for." action={<button className="button" onClick={() => onNavigate('availableRequests')}>Browse requests</button>} />
    <div className="stats-grid"><Stat label="Available requests" value={requests.length} note="Open requests from customers"/><Stat label="My jobs" value={jobs.length} note="Bids selected by customers"/><Stat label="Your role" value="Worker" note="Submit clear, competitive bids"/></div>
    <section className="card section-card"><div className="section-title"><div><h2>Available work</h2><p>Open requests from customers</p></div><button className="text-button" onClick={() => onNavigate('availableRequests')}>Browse all →</button></div>
      {requests.length ? requests.slice(0, 4).map((request) => <RequestRow key={request._id} request={request} action={<button className="button button-outline button-small" onClick={() => onNavigate('availableRequests')}>View request</button>} />) : <EmptyState title="No open requests" text="New customer requests will appear here." />}
    </section>
  </>;
}

function AvailableRequestsPage({ requests, onSelect }) {
  return <><PageHeading eyebrow="Worker workspace" title="Available requests" description="Choose a request that matches the services you provide." />
    <section className="card section-card">{requests.length ? requests.map((request) => <RequestRow key={request._id} request={request} action={<button className="button button-small" onClick={() => onSelect(request)}>View request →</button>} />) : <EmptyState title="No open requests right now" text="Check back later for new work from customers." />}</section>
  </>;
}

function SubmitBidPage({ request, onSubmit, onBack }) {
  return <><PageHeading eyebrow="Worker workspace" title="View request and submit bid" description="Review the details, then send your price and a short message." action={<button className="button button-outline" onClick={onBack}>← Available requests</button>} />
    {request && <><section className="card request-summary"><span className="eyebrow">{request.service?.name || 'Service request'}</span><h2>{request.description}</h2><p>Customer: {request.customer?.name || 'Customer'} <span>·</span> Location: {request.location} <span>·</span> <StatusBadge status={request.status}/></p></section>
    {request.status === 'OPEN' ? <section className="card form-card"><h2>Submit your bid</h2><form className="form-stack" onSubmit={onSubmit}><label>Your price<input name="price" type="number" min="1" step="1" required placeholder="Enter amount in rupees" /></label><label>Message to customer<textarea name="message" required rows="4" maxLength="500" placeholder="Introduce yourself and explain how you can help." /></label><button className="button">Send bid</button></form></section> : <p className="message message-success">This request is no longer open for bids.</p>}</>}
  </>;
}

function MyJobsPage({ jobs }) {
  return <><PageHeading eyebrow="Worker workspace" title="My jobs" description="Requests where the customer selected your bid." />
    <section className="card section-card">{jobs.length ? jobs.map((bid) => <article className="job-row" key={bid._id}><div><span className="eyebrow">{bid.request?.service?.name || 'Selected job'}</span><h3>{bid.request?.description}</h3><p>Customer: {bid.request?.customer?.name || 'Customer'} <span>·</span> {bid.request?.location}</p></div><div className="job-price"><strong>{money(bid.price)}</strong><StatusBadge status={bid.request?.status}/></div></article>) : <EmptyState title="No selected jobs yet" text="When a customer selects your bid, the job will show up here." />}</section>
  </>;
}

function AdminDashboard({ tab, setTab, users, requests, services, onCreateService, onCreateAdmin }) {
  return <><PageHeading eyebrow="Administration" title="Admin dashboard" description="Review registered accounts and service requests." />
    <div className="stats-grid"><Stat label="Users" value={users.length} note="Registered accounts"/><Stat label="Requests" value={requests.length} note="All service requests"/><Stat label="Services" value={services.length} note="Available service types"/></div>
    <div className="tab-bar">{[['requests', 'View requests'], ['users', 'View users'], ['services', 'Manage services']].map(([key, label]) => <button key={key} className={tab === key ? 'tab active' : 'tab'} onClick={() => setTab(key)}>{label}</button>)}</div>
    {tab === 'requests' && <section className="card section-card"><h2>All service requests</h2>{requests.length ? requests.map((request) => <RequestRow key={request._id} request={request} />) : <EmptyState title="No requests yet" text="Customer requests will appear here." />}</section>}
    {tab === 'users' && <section className="admin-users"><div className="card form-card"><h2>Add an admin</h2><p className="admin-help">Only a signed-in admin can create another admin account.</p><form className="form-stack" onSubmit={onCreateAdmin}><label>Full name<input name="name" required placeholder="Admin name" /></label><label>Email address<input name="email" type="email" required placeholder="admin@example.com" /></label><label>Password<input name="password" type="password" required minLength="6" placeholder="At least 6 characters" /></label><button className="button">Create admin</button></form></div><div className="card section-card"><h2>Registered users</h2>{users.length ? users.map((person) => <article className="user-row" key={person.id || person._id}><div className="worker-avatar">{person.name?.[0]?.toUpperCase()}</div><div><b>{person.name}</b><p>{person.email}</p></div><span className="role-pill">{person.role}</span></article>) : <EmptyState title="No accounts found" text="Registered customers and workers will appear here." />}</div></section>}
    {tab === 'services' && <section className="admin-services"><div className="card form-card"><h2>Add a service</h2><form className="form-stack" onSubmit={onCreateService}><label>Service name<input name="name" required placeholder="Plumbing" /></label><label>Description<textarea name="description" required rows="3" placeholder="Describe this service" /></label><button className="button">Add service</button></form></div><div className="card section-card"><h2>Available services</h2><div className="service-grid">{services.map((service) => <article className="service-row service-card" key={service._id}><span className="service-card-icon">{service.name?.[0]?.toUpperCase() || 'S'}</span><div><b>{service.name}</b><p>{service.description}</p></div></article>)}</div></div></section>}
  </>;
}

function RequestRow({ request, action }) {
  return <article className="request-row"><div className="request-symbol">{request.service?.name?.[0]?.toUpperCase() || 'S'}</div><div className="request-copy"><div className="request-title-line"><h3>{request.service?.name || 'Service request'}</h3><StatusBadge status={request.status}/></div><p>{request.description}</p><small>{request.customer?.name ? `Customer: ${request.customer.name} · ` : ''}{request.location}</small></div>{action}</article>;
}

function StatusBadge({ status }) {
  const style = status === 'OPEN' || status === 'PENDING' ? 'status-open' : status === 'SELECTED' ? 'status-selected' : 'status-muted';
  return <span className={`status-badge ${style}`}>{status || '—'}</span>;
}
