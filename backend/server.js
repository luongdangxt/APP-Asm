const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// Kết nối MongoDB
mongoose.connect('mongodb://localhost:27017/dang', { useNewUrlParser: true, useUnifiedTopology: true })
  .then(() => console.log('✅ MongoDB connected'))
  .catch(err => console.log('❌ DB Error:', err));

// Ví dụ model
const Note = mongoose.model('Note', new mongoose.Schema({
  title: String,
  content: String
}));

// API: Lấy danh sách ghi chú
app.get('/notes', async (req, res) => {
  const notes = await Note.find();
  res.json(notes);
});

// API: Thêm ghi chú
app.post('/notes', async (req, res) => {
  const note = new Note(req.body);
  await note.save();
  res.json({ message: 'Note added!', note });
});

app.listen(3000, () => console.log('🚀 Server running on http://localhost:3000'));
