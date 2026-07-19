// Vercel serverless function — sends a booking SMS notification to Kelsey via Twilio.
// Uses the HCiHY Twilio subaccount (created 2026-07-05), same one wired for
// Leilani's Classy Cleaning (see leilanis-classy-cleaning/api/notify-sms.js).

const TWILIO_ACCOUNT_SID = process.env.TWILIO_ACCOUNT_SID;
const TWILIO_AUTH_TOKEN = process.env.TWILIO_AUTH_TOKEN;
const TWILIO_FROM_NUMBER = process.env.TWILIO_FROM_NUMBER;

// Kelsey's own phone. This is the same number as the public site's tel:/sms:
// links — Kelsey's personal number and the business number are one and the
// same. Confirmed by Dan 2026-07-19.
const KELSEY_PHONE = '+16614366728';

function toE164(phone) {
  const digits = String(phone || '').replace(/\D/g, '');
  if (digits.length === 10) return `+1${digits}`;
  if (digits.length === 11 && digits.startsWith('1')) return `+${digits}`;
  return null;
}

async function sendSms(to, body) {
  const auth = Buffer.from(`${TWILIO_ACCOUNT_SID}:${TWILIO_AUTH_TOKEN}`).toString('base64');
  const params = new URLSearchParams({ To: to, From: TWILIO_FROM_NUMBER, Body: body });

  const res = await fetch(
    `https://api.twilio.com/2010-04-01/Accounts/${TWILIO_ACCOUNT_SID}/Messages.json`,
    {
      method: 'POST',
      headers: {
        Authorization: `Basic ${auth}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: params,
    }
  );

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Twilio ${res.status}: ${text}`);
  }
}

module.exports = async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ ok: false, error: 'Method not allowed' });
    return;
  }

  const { name, phone, day, time, service, notes } = req.body || {};

  if (!name || !phone || !day || !time) {
    res.status(400).json({ ok: false, error: 'Missing required fields' });
    return;
  }

  if (!TWILIO_ACCOUNT_SID || !TWILIO_AUTH_TOKEN || !TWILIO_FROM_NUMBER) {
    console.error('Twilio env vars not configured — cannot send booking SMS');
    res.status(500).json({ ok: false, error: 'SMS notification is not configured yet' });
    return;
  }

  const clientPhone = toE164(phone) || phone;
  const body = `New booking request — ${name}, ${service || 'service TBD'}, ${day} @ ${time}. Phone: ${clientPhone}${notes ? `. Notes: ${notes}` : ''}`;

  try {
    await sendSms(KELSEY_PHONE, body);
    res.status(200).json({ ok: true });
  } catch (err) {
    console.error('Kelsey SMS failed:', err.message);
    res.status(502).json({ ok: false, error: 'Failed to send SMS notification' });
  }
};
