const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const bcrypt = require('bcryptjs');

const app = express();
app.use(cors());
app.use(express.json());

// Connect MongoDB
mongoose.connect('mongodb://localhost:27017/dang', { useNewUrlParser: true, useUnifiedTopology: true })
  .then(() => console.log('MongoDB connected'))
  .catch(err => console.log('DB Error:', err));

// User model for auth (email optional so mobile app keeps username/password fields)
const userSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true, trim: true },
  email: { type: String, unique: true, lowercase: true, trim: true, sparse: true },
  passwordHash: { type: String, required: true },
  clientId: { type: Number, unique: true, index: true },
  createdAt: { type: Date, default: Date.now }
});

const User = mongoose.model('User', userSchema);

// Note model
const Note = mongoose.model('Note', new mongoose.Schema({
  title: String,
  content: String
}));

// Auth: register
app.post('/auth/register', async (req, res) => {
  try {
    const { username, email, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({ message: 'username and password are required' });
    }

    const search = [{ username }];
    if (email) search.push({ email });

    const existingUser = await User.findOne({ $or: search });
    if (existingUser) {
      return res.status(409).json({ message: 'User already exists' });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const last = await User.findOne().sort({ clientId: -1 }).select('clientId').lean();
    const nextClientId = last && typeof last.clientId === 'number' ? last.clientId + 1 : 1;
    const user = await User.create({ username, email, passwordHash, clientId: nextClientId });

    return res.status(201).json({
      message: 'Registered successfully',
      user: { id: user.clientId, username: user.username, email: user.email }
    });
  } catch (err) {
    console.error('Register error', err);
    return res.status(500).json({ message: 'Unexpected error' });
  }
});

// Auth: login
app.post('/auth/login', async (req, res) => {
  try {
    const { username, email, password } = req.body;

    if ((!username && !email) || !password) {
      return res.status(400).json({ message: 'username (or email) and password are required' });
    }

    const query = username ? { username } : { email };
    const user = await User.findOne(query);
    if (!user) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Assign a clientId for older accounts if missing
    if (typeof user.clientId !== 'number') {
      const last = await User.findOne().sort({ clientId: -1 }).select('clientId').lean();
      const nextClientId = last && typeof last.clientId === 'number' ? last.clientId + 1 : 1;
      user.clientId = nextClientId;
      await user.save();
    }

    const isMatch = await bcrypt.compare(password, user.passwordHash);
    if (!isMatch) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    return res.json({
      message: 'Login successful',
      user: { id: user.clientId, username: user.username, email: user.email }
    });
  } catch (err) {
    console.error('Login error', err);
    return res.status(500).json({ message: 'Unexpected error' });
  }
});

// Auth: fetch user by numeric clientId (for "remember me" lookup)
app.get('/auth/user/:id', async (req, res) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isFinite(id) || id <= 0) {
      return res.status(400).json({ message: 'Invalid id' });
    }
    const user = await User.findOne({ clientId: id }).lean();
    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }
    return res.json({ user: { id: user.clientId, username: user.username, email: user.email } });
  } catch (err) {
    console.error('Fetch user error', err);
    return res.status(500).json({ message: 'Unexpected error' });
  }
});

// Notes: list
app.get('/notes', async (req, res) => {
  const notes = await Note.find();
  res.json(notes);
});

// Notes: create
app.post('/notes', async (req, res) => {
  const note = new Note(req.body);
  await note.save();
  res.json({ message: 'Note added!', note });
});

app.listen(3000, () => console.log('Server running on http://localhost:3000'));
