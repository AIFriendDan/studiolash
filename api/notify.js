const { Resend } = require('resend');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }

  const { name, phone, email, day, time, service, notes } = req.body || {};

  if (!name || !phone || !day || !time) {
    res.status(400).json({ error: 'Missing required fields' });
    return;
  }

  if (!process.env.RESEND_API_KEY) {
    console.warn('RESEND_API_KEY is not set. Booking request received but not emailed.');
    res.status(500).json({ error: 'Notification email is not configured yet' });
    return;
  }

  const resend = new Resend(process.env.RESEND_API_KEY);
  const fromEmail = process.env.RESEND_FROM_EMAIL || 'bookings@hcihysvc.com';
  const notifyTo = process.env.NOTIFY_EMAIL || 'dan.garza@hcihysvc.com';

  const bodyLines = [
    `New booking request — Kelsey Renée Beauty`,
    ``,
    `Name: ${name}`,
    `Phone: ${phone}`,
    `Email: ${email || 'n/a'}`,
    `Service: ${service || 'n/a'}`,
    `Requested day: ${day}`,
    `Requested time: ${time}`,
    `Notes: ${notes || 'n/a'}`,
  ];

  try {
    const { error } = await resend.emails.send({
      from: `Kelsey Renée Beauty Booking <${fromEmail}>`,
      to: [notifyTo],
      subject: `New booking request — ${name} (${day} @ ${time})`,
      html: bodyLines.map((line) => `<p>${line}</p>`).join(''),
    });

    if (error) {
      console.error('Resend error:', error);
      res.status(502).json({ error: 'Failed to send notification email' });
      return;
    }

    res.status(200).json({ ok: true });
  } catch (err) {
    console.error('Notify handler error:', err);
    res.status(500).json({ error: 'Unexpected server error' });
  }
};
